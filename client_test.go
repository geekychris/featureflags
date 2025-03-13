package client

import (
	"errors"
	"testing"
	"time"

	"github.com/bradfitz/gomemcache/memcache"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
)

// MockMemcachedClient is a mock implementation of the memcache client
type MockMemcachedClient struct {
	mock.Mock
}

func (m *MockMemcachedClient) Get(key string) (*memcache.Item, error) {
	args := m.Called(key)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*memcache.Item), args.Error(1)
}

func (m *MockMemcachedClient) Set(item *memcache.Item) error {
	args := m.Called(item)
	return args.Error(0)
}

func (m *MockMemcachedClient) Delete(key string) error {
	args := m.Called(key)
	return args.Error(0)
}

func (m *MockMemcachedClient) GetMulti(keys []string) (map[string]*memcache.Item, error) {
	args := m.Called(keys)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(map[string]*memcache.Item), args.Error(1)
}

func (m *MockMemcachedClient) FlushAll() error {
	args := m.Called()
	return args.Error(0)
}

// TestFeatureFlagRetrieval tests the basic feature flag retrieval functionality
func TestFeatureFlagRetrieval(t *testing.T) {
	tests := []struct {
		name          string
		entityID      string
		setupMock     func(mock *MockMemcachedClient)
		expectedFlags map[string]string
		expectedError error
	}{
		{
			name:     "Successfully retrieve feature flags",
			entityID: "user123",
			setupMock: func(m *MockMemcachedClient) {
				// Prepare a JSON response that would be stored in memcached
				jsonData := `{"flagA":"valueA","flagB":"valueB"}`
				m.On("Get", "featureflag:user123").Return(&memcache.Item{
					Key:        "featureflag:user123",
					Value:      []byte(jsonData),
					Flags:      0,
					Expiration: 3600,
				}, nil)
			},
			expectedFlags: map[string]string{
				"flagA": "valueA",
				"flagB": "valueB",
			},
			expectedError: nil,
		},
		{
			name:     "Handle cache miss",
			entityID: "user456",
			setupMock: func(m *MockMemcachedClient) {
				m.On("Get", "featureflag:user456").Return(nil, memcache.ErrCacheMiss)
			},
			expectedFlags: nil,
			expectedError: memcache.ErrCacheMiss,
		},
		{
			name:     "Handle server error",
			entityID: "user789",
			setupMock: func(m *MockMemcachedClient) {
				m.On("Get", "featureflag:user789").Return(nil, errors.New("server error"))
			},
			expectedFlags: nil,
			expectedError: errors.New("server error"),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := new(MockMemcachedClient)
			tt.setupMock(mockClient)

			client := &FeatureFlagClient{
				memcachedClient: mockClient,
				prefix:          "featureflag:",
				timeout:         time.Second * 5,
			}

			flags, err := client.GetFeatureFlags(tt.entityID)

			if tt.expectedError != nil {
				assert.Error(t, err)
				assert.Equal(t, tt.expectedError.Error(), err.Error())
			} else {
				assert.NoError(t, err)
				assert.Equal(t, tt.expectedFlags, flags)
			}

			mockClient.AssertExpectations(t)
		})
	}
}

// TestFeatureFlagMerging tests merging feature flags from multiple sources
func TestFeatureFlagMerging(t *testing.T) {
	tests := []struct {
		name          string
		entityIDs     []string
		setupMock     func(mock *MockMemcachedClient)
		expectedFlags map[string]string
		expectedError error
	}{
		{
			name:      "Successfully merge feature flags",
			entityIDs: []string{"user123", "group456"},
			setupMock: func(m *MockMemcachedClient) {
				m.On("GetMulti", []string{"featureflag:user123", "featureflag:group456"}).Return(map[string]*memcache.Item{
					"featureflag:user123": {
						Key:        "featureflag:user123",
						Value:      []byte(`{"flagA":"valueA","flagB":"valueB"}`),
						Flags:      0,
						Expiration: 3600,
					},
					"featureflag:group456": {
						Key:        "featureflag:group456",
						Value:      []byte(`{"flagB":"overriddenB","flagC":"valueC"}`),
						Flags:      0,
						Expiration: 3600,
					},
				}, nil)
			},
			expectedFlags: map[string]string{
				"flagA": "valueA",
				"flagB": "overriddenB", // Last one wins in merge
				"flagC": "valueC",
			},
			expectedError: nil,
		},
		{
			name:      "Handle partial cache miss",
			entityIDs: []string{"user123", "missing456"},
			setupMock: func(m *MockMemcachedClient) {
				m.On("GetMulti", []string{"featureflag:user123", "featureflag:missing456"}).Return(map[string]*memcache.Item{
					"featureflag:user123": {
						Key:        "featureflag:user123",
						Value:      []byte(`{"flagA":"valueA","flagB":"valueB"}`),
						Flags:      0,
						Expiration: 3600,
					},
				}, nil)
			},
			expectedFlags: map[string]string{
				"flagA": "valueA",
				"flagB": "valueB",
			},
			expectedError: nil,
		},
		{
			name:      "Handle empty input",
			entityIDs: []string{},
			setupMock: func(m *MockMemcachedClient) {
				// No calls expected
			},
			expectedFlags: map[string]string{},
			expectedError: nil,
		},
		{
			name:      "Handle server error",
			entityIDs: []string{"user123", "group456"},
			setupMock: func(m *MockMemcachedClient) {
				m.On("GetMulti", []string{"featureflag:user123", "featureflag:group456"}).Return(nil, errors.New("server error"))
			},
			expectedFlags: nil,
			expectedError: errors.New("server error"),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := new(MockMemcachedClient)
			tt.setupMock(mockClient)

			client := &FeatureFlagClient{
				memcachedClient: mockClient,
				prefix:          "featureflag:",
				timeout:         time.Second * 5,
			}

			flags, err := client.MergeFeatureFlags(tt.entityIDs)

			if tt.expectedError != nil {
				assert.Error(t, err)
				assert.Equal(t, tt.expectedError.Error(), err.Error())
			} else {
				assert.NoError(t, err)
				assert.Equal(t, tt.expectedFlags, flags)
			}

			mockClient.AssertExpectations(t)
		})
	}
}

// TestErrorHandling tests various error handling scenarios
func TestErrorHandling(t *testing.T) {
	tests := []struct {
		name          string
		setup         func(client *FeatureFlagClient, mock *MockMemcachedClient)
		action        func(client *FeatureFlagClient) (interface{}, error)
		expectedError error
	}{
		{
			name: "Handle invalid JSON response",
			setup: func(client *FeatureFlagClient, mock *MockMemcachedClient) {
				mock.On("Get", "featureflag:user123").Return(&memcache.Item{
					Key:        "featureflag:user123",
					Value:      []byte(`{invalid json}`),
					Flags:      0,
					Expiration: 3600,
				}, nil)
			},
			action: func(client *FeatureFlagClient) (interface{}, error) {
				return client.GetFeatureFlags("user123")
			},
			expectedError: errors.New("invalid character 'i' looking for beginning of object key string"),
		},
		{
			name: "Handle timeout",
			setup: func(client *FeatureFlagClient, mock *MockMemcachedClient) {
				// Simulate timeout by setting a very short timeout and not returning from the mock
				client.timeout = time.Nanosecond
				mock.On("Get", "featureflag:user123").Return(nil, errors.New("timeout"))
			},
			action: func(client *FeatureFlagClient) (interface{}, error) {
				return client.GetFeatureFlags("user123")
			},
			expectedError: errors.New("timeout"),
		},
		{
			name: "Handle nil entity ID",
			setup: func(client *FeatureFlagClient, mock *MockMemcachedClient) {
				// No mock setup needed
			},
			action: func(client *FeatureFlagClient) (interface{}, error) {
				return client.GetFeatureFlags("")
			},
			expectedError: errors.New("entity ID cannot be empty"),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := new(MockMemcachedClient)
			client := &FeatureFlagClient{
				memcachedClient: mockClient,
				prefix:          "featureflag:",
				timeout:         time.Second * 5,
			}

			tt.setup(client, mockClient)

			_, err := tt.action(client)

			assert.Error(t, err)
			if tt.expectedError != nil {
				assert.Contains(t, err.Error(), tt.expectedError.Error())
			}

			mockClient.AssertExpectations(t)
		})
	}
}

// TestConnectionHandling tests connection handling behavior
func TestConnectionHandling(t *testing.T) {
	tests := []struct {
		name          string
		setupMock     func(mock *MockMemcachedClient)
		config        *FeatureFlagClientConfig
		expectedError bool
	}{
		{
			name: "Successful connection",
			setupMock: func(m *MockMemcachedClient) {
				// No setup needed for constructor test
			},
			config: &FeatureFlagClientConfig{
				Servers:  []string{"localhost:11211"},
				Prefix:   "featureflag:",
				Timeout:  5,
				MaxIdle:  10,
				PoolSize: 10,
			},
			expectedError: false,
		},
		{
			name: "Handle empty server list",
			setupMock: func(m *MockMemcachedClient) {
				// No setup needed for constructor test
			},
			config: &FeatureFlagClientConfig{
				Servers:  []string{},
				Prefix:   "featureflag:",
				Timeout:  5,
				MaxIdle:  10,
				PoolSize: 10,
			},
			expectedError: true,
		},
		{
			name: "Handle invalid timeout",
			setupMock: func(m *MockMemcachedClient) {
				// No setup needed for constructor test
			},
			config: &FeatureFlagClientConfig{
				Servers:  []string{"localhost:11211"},
				Prefix:   "featureflag:",
				Timeout:  -1,
				MaxIdle:  10,
				PoolSize: 10,
			},
			expectedError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockClient := new(MockMemcachedClient)
			tt.setupMock(mockClient)

			client, err := NewFeatureFlagClientWithClient(tt.config, mockClient)

			if tt.expectedError {
				assert.Error(t, err)
				assert.Nil(t, client)
			} else {
				assert.NoError(t, err)
				assert.NotNil(t, client)
				if client != nil {
					assert.Equal(t, tt.config.Prefix, client.prefix)
					assert.Equal(t, time.Duration(tt.config.Timeout)*time.Second, client.timeout)
				}
			}
		})
	}
}

// TestFeatureFlagCaching tests caching behavior
func TestFeatureFlagCaching(t *testing.T) {
	mockClient := new(MockMemcachedClient)
	
	// Setup mock to return different values on consecutive calls
	firstCall := true
	mockClient.On("Get", "featureflag:user123").Return(func(key string) *memcache.Item {
		if firstCall {
			firstCall = false
			return &memcache.Item{
				Key:        key,
				Value:      []byte(`{"flagA":"valueA","flagB":"valueB"}`),
				Flags:      0,
				Expiration: 3600,
			}
		}
		return &memcache.Item{
			Key:        key,
			Value:      []byte(`{"flagA":"updatedA","flagB":"updatedB"}`),
			Flags:      0,
			Expiration

