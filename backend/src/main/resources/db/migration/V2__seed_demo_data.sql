-- 智慧物业管理系统 SaaS 多租户版初始化演示数据
USE property_saas;

INSERT INTO sys_tenant_package(package_id, package_name, package_code, price, billing_cycle, enabled_modules, quotas, status)
VALUES
(1, '标准版', 'STANDARD', 9800.00, 'YEAR', JSON_ARRAY('base','fee','payment','service','report'), JSON_OBJECT('project',5,'user',50,'house',5000,'storageMb',10240,'sms',5000), 'ACTIVE'),
(2, '旗舰版', 'ULTIMATE', 19800.00, 'YEAR', JSON_ARRAY('base','fee','payment','service','lease','patrol','device','report'), JSON_OBJECT('project',20,'user',200,'house',30000,'storageMb',102400,'sms',30000), 'ACTIVE');

INSERT INTO sys_tenant(tenant_id, tenant_name, tenant_code, contact_name, contact_mobile, package_id, service_start_date, service_end_date, status)
VALUES
(1, '演示物业A公司', 'TENANT_A', '张经理', '13800000001', 2, '2026-06-01', '2027-05-31', 'ACTIVE'),
(2, '演示物业B公司', 'TENANT_B', '李经理', '13800000002', 1, '2026-06-01', '2027-05-31', 'ACTIVE');

INSERT INTO sys_tenant_config(config_id, tenant_id, logo_url, service_phone)
VALUES
(1, 1, '', '0718-0000001'),
(2, 2, '', '0718-0000002');

INSERT INTO tenant_pay_config(pay_config_id, tenant_id, project_id, pay_channel, merchant_mode, mch_id, sub_mch_id, app_id, api_v3_key_encrypted, cert_serial_no, settlement_mode, notify_url, status)
VALUES
(1, 1, NULL, 'WECHAT', 'NORMAL_MCH', 'demo_mch_1001', NULL, 'wx_demo_app_1001', 'change_me_encrypted', 'demo_cert_1001', 'TENANT_DIRECT', 'https://api.example.com/api/payment/wechat/notify', 'ACTIVE'),
(2, 2, NULL, 'WECHAT', 'NORMAL_MCH', 'demo_mch_2001', NULL, 'wx_demo_app_2001', 'change_me_encrypted', 'demo_cert_2001', 'TENANT_DIRECT', 'https://api.example.com/api/payment/wechat/notify', 'ACTIVE');

INSERT INTO sys_user(user_id, tenant_id, username, real_name, mobile, password_hash, user_type, status)
VALUES
(1, NULL, 'platform_admin', '平台管理员', '13900000000', '$2a$10$change_me', 'PLATFORM', 'ACTIVE'),
(1001, 1, 'tenant_a_admin', 'A公司管理员', '13900000001', '$2a$10$change_me', 'TENANT', 'ACTIVE'),
(2001, 2, 'tenant_b_admin', 'B公司管理员', '13900000002', '$2a$10$change_me', 'TENANT', 'ACTIVE');

INSERT INTO sys_role(role_id, tenant_id, role_name, role_code, role_level, data_scope, status)
VALUES
(1, NULL, '平台超级管理员', 'PLATFORM_ADMIN', 'PLATFORM', 'ALL_TENANT', 'ACTIVE'),
(1001, 1, '租户管理员', 'TENANT_ADMIN', 'TENANT', 'ALL_TENANT', 'ACTIVE'),
(2001, 2, '租户管理员', 'TENANT_ADMIN', 'TENANT', 'ALL_TENANT', 'ACTIVE');

INSERT INTO sys_user_role(id, tenant_id, user_id, role_id)
VALUES
(1, NULL, 1, 1),
(1001, 1, 1001, 1001),
(2001, 2, 2001, 2001);

INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(1, 0, '租户管理', 'API', 'platform:tenant:list', '/platform/tenants', '/api/platform/tenants', 'platform', 10, 1, 'ACTIVE'),
(2, 0, '项目管理', 'API', 'base:project:list', '/base/projects', '/api/base/projects', 'base', 20, 1, 'ACTIVE'),
(3, 0, '房屋管理', 'API', 'base:house:list', '/base/houses', '/api/base/houses', 'base', 30, 1, 'ACTIVE'),
(4, 0, '账单管理', 'API', 'fee:bill:list', '/fee/bills', '/api/fee/bills', 'fee', 40, 1, 'ACTIVE'),
(5, 0, '退款管理', 'API', 'payment:refund:create', '/payment/refunds', '/api/payment/refunds', 'payment', 50, 1, 'ACTIVE'),
(6, 0, '工单派单', 'API', 'service:workorder:dispatch', '/service/workorders', '/api/service/workorders/{workOrderId}/dispatch', 'service', 60, 1, 'ACTIVE'),
(7, 0, '报表查看', 'API', 'report:fee:view', '/reports', '/api/report/fee/summary', 'report', 70, 1, 'ACTIVE');

INSERT INTO sys_role_menu(id, tenant_id, role_id, menu_id)
VALUES
(1, NULL, 1, 1),
(1001, 1, 1001, 2),
(1002, 1, 1001, 3),
(1003, 1, 1001, 4),
(1004, 1, 1001, 5),
(1005, 1, 1001, 6),
(1006, 1, 1001, 7),
(2001, 2, 2001, 2),
(2002, 2, 2001, 3),
(2003, 2, 2001, 4),
(2004, 2, 2001, 7);

INSERT INTO base_project(project_id, tenant_id, project_code, project_name, project_type, province, city, district, address, service_phone, status)
VALUES
(101, 1, 'A-PROJ-001', 'A公司阳光小区', 'RESIDENTIAL', '湖北省', '恩施州', '恩施市', '演示地址1号', '0718-1111111', 'ACTIVE'),
(201, 2, 'B-PROJ-001', 'B公司幸福小区', 'RESIDENTIAL', '湖北省', '恩施州', '恩施市', '演示地址2号', '0718-2222222', 'ACTIVE');

INSERT INTO base_building(building_id, tenant_id, project_id, building_name, building_code, floor_count, status)
VALUES
(10001, 1, 101, '1栋', 'A1', 18, 'ACTIVE'),
(20001, 2, 201, '1栋', 'B1', 18, 'ACTIVE');

INSERT INTO base_unit(unit_id, tenant_id, project_id, building_id, unit_name, status)
VALUES
(100001, 1, 101, 10001, '1单元', 'ACTIVE'),
(200001, 2, 201, 20001, '1单元', 'ACTIVE');

INSERT INTO base_house(house_id, tenant_id, project_id, building_id, unit_id, house_no, floor_no, building_area, house_usage, house_status)
VALUES
(1000001, 1, 101, 10001, 100001, '101', 1, 100.00, 'RESIDENTIAL', 'OCCUPIED'),
(2000001, 2, 201, 20001, 200001, '101', 1, 120.00, 'RESIDENTIAL', 'OCCUPIED');

INSERT INTO fee_item(item_id, tenant_id, item_code, item_name, item_type, status)
VALUES
(11001, 1, 'PROPERTY_FEE', '物业费', 'PERIODIC', 'ACTIVE'),
(21001, 2, 'PROPERTY_FEE', '物业费', 'PERIODIC', 'ACTIVE');

INSERT INTO fee_standard(standard_id, tenant_id, project_id, item_id, standard_name, charge_method, unit_price, cycle, effective_date, status)
VALUES
(12001, 1, 101, 11001, '住宅物业费标准', 'AREA', 1.5000, 'MONTH', '2026-01-01', 'ACTIVE'),
(22001, 2, 201, 21001, '住宅物业费标准', 'AREA', 1.8000, 'MONTH', '2026-01-01', 'ACTIVE');

INSERT INTO member_user(member_id, tenant_id, openid, unionid, mobile, real_name, status)
VALUES
(10001, 1, 'openid_tenant_a_10001', 'union_demo_10001', '13810000001', '王业主', 'ACTIVE'),
(20001, 2, 'openid_tenant_b_20001', 'union_demo_20001', '13820000001', '赵业主', 'ACTIVE');

INSERT INTO member_house_bind(bind_id, tenant_id, project_id, member_id, house_id, bind_role, real_name, mobile, status, effective_date)
VALUES
(13001, 1, 101, 10001, 1000001, 'OWNER', '王业主', '13810000001', 'APPROVED', '2026-06-01'),
(23001, 2, 201, 20001, 2000001, 'OWNER', '赵业主', '13820000001', 'APPROVED', '2026-06-01');

INSERT INTO fee_bill(bill_id, tenant_id, project_id, bill_no, item_id, standard_id, object_type, object_id, member_id, house_id, bill_period, receivable_amount, discount_amount, paid_amount, refund_amount, remaining_amount, due_date, status, source_type)
VALUES
(14001, 1, 101, 'BILL-A-202606-0001', 11001, 12001, 'HOUSE', 1000001, 10001, 1000001, '2026-06', 150.00, 0.00, 0.00, 0.00, 150.00, '2026-06-30', 'UNPAID', 'GENERATED'),
(24001, 2, 201, 'BILL-B-202606-0001', 21001, 22001, 'HOUSE', 2000001, 20001, 2000001, '2026-06', 216.00, 0.00, 0.00, 0.00, 216.00, '2026-06-30', 'UNPAID', 'GENERATED');

INSERT INTO pay_order(order_id, tenant_id, project_id, order_no, member_id, pay_channel, amount, subject, status, expire_at)
VALUES
(15001, 1, 101, 'PAY-A-202606-0001', 10001, 'WECHAT', 150.00, 'A公司阳光小区2026-06物业费', 'PENDING', '2026-06-30 23:59:59');

INSERT INTO pay_order_bill(id, tenant_id, project_id, order_id, bill_id, amount)
VALUES
(1500101, 1, 101, 15001, 14001, 150.00);
