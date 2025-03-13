package featureflag

import (
	"log"
	"time"
)

// Config holds the configuration for the feature flag client
type Config struct {
	// Servers is a list of memcached server addresses (host:port)
	Servers []string
	
	// Timeout is the connection timeout for memcached operations
	Timeout time.Duration
	
	// MaxIdleConnections is the maximum number of idle connections
	MaxIdleConnections int
	
	// KeyPrefix is the prefix for cache keys (defaults to "feature_flags:")
	KeyPrefix string
	
	// Logger is the logger to use for logging messages
	Logger *log.Logger
}

// DefaultConfig returns a default configuration
func DefaultConfig() *Config {
	return &Config{
		Servers:            []string{"localhost:11211"},
		Timeout:            1 * time.Second,
		MaxIdleConnections: 2,
		KeyPrefix:          "feature_flags:",
		Logger:             log.Default(),
	}
}

// WithServers sets the memcached servers and returns the config
func (c *Config) WithServers(servers ...string) *Config {
	c.Servers = servers
	return c
}

// WithTimeout sets the timeout and returns the config
func (c *Config) WithTimeout(timeout time.Duration) *Config {
	c.Timeout = timeout
	return c
}

// WithMaxIdleConnections sets the maximum idle connections and returns the config
func (c *Config) WithMaxIdleConnections(max int) *Config {
	c.MaxIdleConnections = max
	return c
}

// WithKeyPrefix sets the key prefix and returns the config
func (c *Config) WithKeyPrefix(prefix string) *Config {
	c.KeyPrefix = prefix
	return c
}

// WithLogger sets the logger and returns the config
func (c *Config) WithLogger(logger *log.Logger) *Config {
	c.Logger = logger
	return c
}

