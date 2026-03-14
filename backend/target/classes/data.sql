-- Initialize with a demo user (ID will be auto-generated)
INSERT INTO users (username, email, password, created_at) 
VALUES ('demo', 'demo@finsight.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', CURRENT_TIMESTAMP);

-- Add default role (user_id will be 1 since it's the first user)
INSERT INTO user_roles (user_id, role) VALUES (1, 'USER');
