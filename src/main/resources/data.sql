-- First, check if data exists
DECLARE
v_count NUMBER;
BEGIN
SELECT COUNT(*) INTO v_count FROM identity_entity;

-- Only insert if no data exists
IF v_count = 0 THEN
    INSERT INTO identity_entity (public_guid, user_id)
SELECT 'guid' || LEVEL, 'user' || LEVEL || '@example.com'
FROM DUAL
    CONNECT BY LEVEL <= 5;
END IF;
END;
/