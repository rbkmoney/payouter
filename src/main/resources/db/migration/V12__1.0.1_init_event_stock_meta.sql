INSERT INTO sht.event_stock_meta (last_event_id, last_event_created_at) VALUES((SELECT max(event_id) from sht.payment), now());