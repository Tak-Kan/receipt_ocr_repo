-- Create image table for storing binary image data
CREATE TABLE IF NOT EXISTS `image` (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  filename VARCHAR(255),
  content_type VARCHAR(100),
  data LONGBLOB,
  size BIGINT,
  uploaded_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;