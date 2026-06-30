-- Synthetic seed data so the list/filter views have content immediately. All values are
-- fabricated: TEST- claim ids, MBR-TEST- member ids, obviously-fake patient names. No PHI.

insert into claims
    (claim_id, patient_name, member_id, payer, cpt_code, icd_code,
     billed_amount, paid_amount, balance, status, denial_reason, image_id, created_at)
values
    ('TEST-0001', 'Alpha Tester',     'MBR-TEST-0001', 'Synthetic Health Plan', '99213', 'E11.9',  250.00, 200.00,  50.00, 'submitted', null,                              null, now() - interval '9 day'),
    ('TEST-0002', 'Beta Sample',      'MBR-TEST-0002', 'Synthetic Health Plan', '93000', 'I10',     180.00, 180.00,   0.00, 'paid',      null,                              null, now() - interval '8 day'),
    ('TEST-0003', 'Gamma Placeholder','MBR-TEST-0003', 'Test Mutual',           '70450', 'M54.5',   900.00,   0.00, 900.00, 'denied',    'Prior authorization not on file', null, now() - interval '7 day'),
    ('TEST-0004', 'Delta Demo',       'MBR-TEST-0004', 'Placeholder PPO',       '80053', 'Z00.00',  120.00,   0.00, 120.00, 'pending',   null,                              null, now() - interval '6 day'),
    ('TEST-0005', 'Epsilon Example',  'MBR-TEST-0005', 'Mock Care Network',     '12001', 'J06.9',   340.00, 100.00, 240.00, 'submitted', null,                              null, now() - interval '5 day'),
    ('TEST-0006', 'Zeta Fixture',     'MBR-TEST-0006', 'Test Mutual',           '99214', 'E11.9',   410.00, 410.00,   0.00, 'paid',      null,                              null, now() - interval '4 day'),
    ('TEST-0007', 'Eta Mockup',       'MBR-TEST-0007', 'Synthetic Health Plan', '71046', 'R07.9',   275.00,   0.00, 275.00, 'denied',    'Service not covered under plan',  null, now() - interval '3 day'),
    ('TEST-0008', 'Theta Stub',       'MBR-TEST-0008', 'Placeholder PPO',       '85025', 'D64.9',    95.00,   0.00,  95.00, 'pending',   null,                              null, now() - interval '2 day'),
    ('TEST-0009', 'Iota Dummy',       'MBR-TEST-0009', 'Mock Care Network',     '99213', 'I10',     250.00, 220.00,  30.00, 'submitted', null,                              null, now() - interval '1 day'),
    ('TEST-0010', 'Kappa Synthetic',  'MBR-TEST-0010', 'Synthetic Health Plan', '36415', 'Z00.00',   25.00,  25.00,   0.00, 'paid',      null,                              null, now());
