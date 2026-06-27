ALTER TABLE member_house_bind
  ADD COLUMN relationship VARCHAR(32) NULL COMMENT '与业主关系：SPOUSE/PARENT/CHILD/RELATIVE/TENANT/OTHER' AFTER bind_role,
  ADD COLUMN invite_member_id BIGINT NULL COMMENT '邀请人会员ID' AFTER relationship,
  ADD COLUMN allow_notice TINYINT NOT NULL DEFAULT 1 COMMENT '允许接收通知' AFTER invite_member_id,
  ADD COLUMN allow_bill TINYINT NOT NULL DEFAULT 0 COMMENT '允许查看账单' AFTER allow_notice,
  ADD COLUMN allow_payment TINYINT NOT NULL DEFAULT 0 COMMENT '允许缴费' AFTER allow_bill,
  ADD COLUMN allow_work_order TINYINT NOT NULL DEFAULT 1 COMMENT '允许提交工单' AFTER allow_payment,
  ADD COLUMN allow_visitor TINYINT NOT NULL DEFAULT 1 COMMENT '允许访客通行' AFTER allow_work_order,
  ADD KEY idx_bind_invite_member (tenant_id, invite_member_id, house_id);

INSERT INTO sys_menu(menu_id, parent_id, menu_name, menu_type, permission_code, route_path, api_path, module_code, sort_no, visible, status)
VALUES
(89503, 0, '小程序家属列表', 'API', 'app:family:list', '/pages/family/list', '/api/app/family-members', 'app', 929, 1, 'ACTIVE'),
(89504, 0, '小程序家属管理', 'API', 'app:family:manage', '/pages/family/list', '/api/app/family-members', 'app', 930, 1, 'ACTIVE')
AS new
ON DUPLICATE KEY UPDATE
  menu_name = new.menu_name,
  permission_code = new.permission_code,
  route_path = new.route_path,
  api_path = new.api_path,
  module_code = new.module_code,
  sort_no = new.sort_no,
  visible = new.visible,
  status = new.status;
