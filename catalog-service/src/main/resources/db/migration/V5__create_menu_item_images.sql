

-- 1. Remove the old image_url column
ALTER TABLE menu_items
DROP COLUMN image_url;


-- 2. Create a separate table for menu item images
CREATE TABLE menu_item_images (
                                  id BIGINT NOT NULL AUTO_INCREMENT,

                                  menu_item_id BIGINT NOT NULL,

                                  image_url VARCHAR(1000) NOT NULL,

                                  cloudinary_public_id VARCHAR(255) NOT NULL,

                                  status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',

                                  display_order INT NOT NULL DEFAULT 1,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  PRIMARY KEY (id),

                                  CONSTRAINT fk_menu_item_images_menu_item
                                      FOREIGN KEY (menu_item_id)
                                          REFERENCES menu_items(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT uk_menu_item_display_order
                                      UNIQUE (menu_item_id, display_order),

                                  INDEX idx_menu_item_images_menu_item_id (menu_item_id)
);
