


# Creating the database 
```
CREATE DATABASE feature_flags;
CREATE USER 'feature_flags_user'@'%' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON feature_flags.* TO 'feature_flags_user'@'%';
FLUSH PRIVILEGES;
```

