INSERT INTO identity_entity (id, public_guid, user_id)
SELECT * FROM (
                  SELECT 1, 'guid1guid1guid', 'user1@example.com' UNION ALL
                  SELECT 2, 'guid2guid2guid', 'user2@example.com' UNION ALL
                  SELECT 3, 'guid3guid3guid', 'user3@example.com' UNION ALL
                  SELECT 4, 'guid4guid4guid', 'user4@example.com' UNION ALL
                  SELECT 5, 'guid5guid5guid', 'user5@example.com'
              ) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM identity_entity);