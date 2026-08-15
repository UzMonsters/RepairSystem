ALTER TABLE users ADD COLUMN phone VARCHAR(30) NULL;
ALTER TABLE users ADD COLUMN avatar_attachment_id BIGINT NULL;

ALTER TABLE repair_attachments ALTER COLUMN repair_request_id DROP NOT NULL;

ALTER TABLE repair_attachments DROP CONSTRAINT repair_attachments_type_check;
ALTER TABLE repair_attachments ADD CONSTRAINT repair_attachments_type_check CHECK (
    attachment_type IN ('CUSTOMER_PROBLEM_PHOTO', 'DIAGNOSIS_PHOTO', 'COMPLETION_PHOTO', 'GENERAL_DOCUMENT', 'AVATAR')
);

ALTER TABLE users ADD CONSTRAINT users_avatar_attachment_fk FOREIGN KEY (avatar_attachment_id) REFERENCES repair_attachments(id);
CREATE INDEX idx_users_avatar_attachment_id ON users(avatar_attachment_id);
