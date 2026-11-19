INSERT INTO category (name, color, is_default) VALUES
    ('Food',          '#f97316', FALSE),
    ('Groceries',     '#22c55e', FALSE),
    ('Transport',     '#3b82f6', FALSE),
    ('Shopping',      '#a855f7', FALSE),
    ('Entertainment', '#ec4899', FALSE),
    ('Utilities',     '#eab308', FALSE),
    ('Travel',        '#06b6d4', FALSE),
    ('Health',        '#ef4444', FALSE),
    ('Rent',          '#78716c', FALSE),
    ('Other',         '#64748b', TRUE);

-- CONTAINS = whole-word match anywhere in the vendor name; the most specific (longest) pattern wins,
-- so 'uber eats' -> Food beats 'uber' -> Transport and 'swiggy instamart' -> Groceries beats 'swiggy' -> Food.
INSERT INTO vendor_rule (pattern, match_type, category_id)
SELECT r.pattern, 'CONTAINS', c.id
FROM (VALUES
    ('swiggy', 'Food'), ('zomato', 'Food'), ('dominos', 'Food'), ('domino s', 'Food'), ('mcdonalds', 'Food'),
    ('mcdonald s', 'Food'), ('kfc', 'Food'), ('starbucks', 'Food'), ('uber eats', 'Food'), ('eatsure', 'Food'),
    ('swiggy instamart', 'Groceries'), ('bigbasket', 'Groceries'), ('blinkit', 'Groceries'), ('zepto', 'Groceries'),
    ('dmart', 'Groceries'), ('jiomart', 'Groceries'), ('reliance fresh', 'Groceries'),
    ('uber', 'Transport'), ('ola', 'Transport'), ('rapido', 'Transport'), ('namma yatri', 'Transport'),
    ('indian oil', 'Transport'), ('hp petrol', 'Transport'), ('bharat petroleum', 'Transport'), ('fastag', 'Transport'),
    ('amazon', 'Shopping'), ('flipkart', 'Shopping'), ('myntra', 'Shopping'), ('ajio', 'Shopping'),
    ('nykaa', 'Shopping'), ('decathlon', 'Shopping'), ('croma', 'Shopping'),
    ('netflix', 'Entertainment'), ('spotify', 'Entertainment'), ('bookmyshow', 'Entertainment'),
    ('hotstar', 'Entertainment'), ('prime video', 'Entertainment'), ('pvr', 'Entertainment'),
    ('airtel', 'Utilities'), ('jio', 'Utilities'), ('vodafone', 'Utilities'), ('bescom', 'Utilities'),
    ('tata power', 'Utilities'), ('act fibernet', 'Utilities'), ('electricity', 'Utilities'), ('mahanagar gas', 'Utilities'),
    ('makemytrip', 'Travel'), ('goibibo', 'Travel'), ('irctc', 'Travel'), ('indigo', 'Travel'),
    ('air india', 'Travel'), ('oyo', 'Travel'), ('cleartrip', 'Travel'),
    ('apollo', 'Health'), ('pharmeasy', 'Health'), ('1mg', 'Health'), ('practo', 'Health'), ('cult fit', 'Health'),
    ('rent', 'Rent'), ('nobroker', 'Rent')
) AS r (pattern, category)
JOIN category c ON c.name = r.category;
