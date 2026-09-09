CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,

                        user_id BIGINT NOT NULL,

                        amount DECIMAL(19, 2) NOT NULL,

                        status VARCHAR(30) NOT NULL,

                        created_at TIMESTAMP NOT NULL,

                        updated_at TIMESTAMP NULL
);