package featureflag

import (
	"errors"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/bradfitz/gomemcache/memcache"
)

const defaultCacheKeyPrefix = "feature_flags:"

// FeatureFlag represents a single feature flag
type FeatureFlag struct {
	Name  string
	Value string
}

// FeatureFlagSet represents a set of feature flags for an entity
type FeatureFlagSet map[string]string

// Client is a client for retrieving feature flags from Memcached
type Client struct {
	memcache  *memcache.Client
	timeout   time.Duration
	keyPrefix string
	logger    *log.Logger
}

// NewClient creates a new feature flag client
func NewClient(config *Config) (*Client, error) {
	if config == nil {
		return nil, errors.New("config cannot be nil")
	}

	if len(config.Servers) == 0 {
		return nil, errors.New("at least one memcached server must be specified")
	}

	mc := memcache.New(config.Servers...)
	mc.Timeout = config.Timeout
	mc.MaxIdleConns = config.MaxIdleConnections

	keyPrefix := config.KeyPrefix
	if keyPrefix == "" {
		keyPrefix = defaultCacheKeyPrefix
	}

	return &Client{
		memcache:  mc,
		timeout:   config.Timeout,
		keyPrefix: keyPrefix,
		logger:    config.Logger,
	}, nil
}

// GetFeatureFlags retrieves feature flags for a single entity ID from Memcached
func (c *Client) GetFeatureFlags(entityID string) (FeatureFlagSet, error) {
	if entityID == "" {
		return nil, errors.New("entity ID cannot be empty")
	}

	cacheKey := c.generateCacheKey(entityID)

	item, err := c.memcache.Get(cacheKey)
	if err != nil {
		if err == memcache.ErrCacheMiss {
			c.logWarn("No feature flags found in cache for entity: %s", entityID)
			return make(FeatureFlagSet), nil
		}
		c.logError("Error retrieving feature flags for entity %s: %v", entityID, err)
		return nil, err
	}

	// Decode the item value into a map
	featureFlags, err := deserializeFeatureFlags(item.Value)
	if err != nil {
		c.logError("Error deserializing feature flags for entity %s: %v", entityID, err)
		return make(FeatureFlagSet), nil
	}

	return featureFlags, nil
}

// GetMergedFeatureFlags retrieves and merges feature flags for multiple entity IDs
// Later entities in the list will override flags from earlier entities when there are conflicts
func (c *Client) GetMergedFeatureFlags(entityIDs []string) (FeatureFlagSet, error) {
	if len(entityIDs) == 0 {
		return nil, errors.New("entity ID list cannot be empty")
	}

	// Initialize an empty map for merged flags
	mergedFlags := make(FeatureFlagSet)

	// Process entities in order, allowing later entities to override earlier ones
	for _, entityID := range entityIDs {
		entityFlags, err := c.GetFeatureFlags(entityID)
		if err != nil {
			continue // Skip this entity if there's an error
		}

		// Merge the flags
		for name, value := range entityFlags {
			mergedFlags[name] = value
		}
	}

	return mergedFlags, nil
}

// IsFeatureEnabled checks if a specific feature flag is enabled for an entity
// A feature is considered enabled if it exists and its value is "true" (case-insensitive)
func (c *Client) IsFeatureEnabled(entityID, featureName string, defaultValue bool) bool {
	featureFlags, err := c.GetFeatureFlags(entityID)
	if err != nil {
		return defaultValue
	}

	value, exists := featureFlags[featureName]
	if !exists {
		return defaultValue
	}

	// Convert the value to lowercase and check if it's "true"
	return strings.ToLower(value) == "true"
}

// IsFeatureEnabledMulti checks if a specific feature flag is enabled across multiple entities
// Entities are checked in reverse order, so later entities take precedence
func (c *Client) IsFeatureEnabledMulti(entityIDs []string, featureName string, defaultValue bool) bool {
	if len(entityIDs) == 0 {
		return defaultValue
	}

	// Check entities in reverse order (highest priority first)
	for i := len(entityIDs) - 1; i >= 0; i-- {
		featureFlags, err := c.GetFeatureFlags(entityIDs[i])
		if err != nil {
			continue
		}

		value, exists := featureFlags[featureName]
		if exists {
			return strings.ToLower(value) == "true"
		}
	}

	return defaultValue
}

// GetFeatureValue gets a feature flag value for an entity
func (c *Client) GetFeatureValue(entityID, featureName, defaultValue string) string {
	featureFlags, err := c.GetFeatureFlags(entityID)
	if err != nil {
		return defaultValue
	}

	value, exists := featureFlags[featureName]
	if !exists {
		return defaultValue
	}

	return value
}

// GetFeatureValueMulti gets a feature flag value across multiple entities
// Entities are checked in reverse order, so later entities take precedence
func (c *Client) GetFeatureValueMulti(entityIDs []string, featureName, defaultValue string) string {
	if len(entityIDs) == 0 {
		return defaultValue
	}

	// Check entities in reverse order (highest priority first)
	for i := len(entityIDs) - 1; i >= 0; i-- {
		featureFlags, err := c.GetFeatureFlags(entityIDs[i])
		if err != nil {
			continue
		}

		value, exists := featureFlags[featureName]
		if exists {
			return value
		}
	}

	return defaultValue
}

// Helper methods

func (c *Client) generateCacheKey(entityID string) string {
	return c.keyPrefix + entityID
}

func (c *Client) logError(format string, args ...interface{}) {
	if c.logger != nil {
		c.logger.Printf("ERROR: "+format, args...)
	}
}

func (c *Client) logWarn(format string, args ...interface{}) {
	if c.logger != nil {
		c.logger.Printf("WARN: "+format, args...)
	}
}

// deserializeFeatureFlags deserializes the byte array from Memcached into a FeatureFlagSet
// This implementation assumes a simple JSON encoding, but can be replaced with any serialization format
func deserializeFeatureFlags(data []byte) (FeatureFlagSet, error) {
	// This is a simplified implementation.
	// In a real implementation, you would use proper serialization/deserialization
	// using JSON, Protocol Buffers, or another format based on your needs.
	featureFlags := make(FeatureFlagSet)

	// In this example, we'll assume data is a JSON-encoded map of string to string
	// You would implement proper deserialization here
	err := json.Unmarshal(data, &featureFlags)
	if err != nil {
		return nil, fmt.Errorf("failed to deserialize feature flags: %w", err)
	}

	return featureFlags, nil
}
