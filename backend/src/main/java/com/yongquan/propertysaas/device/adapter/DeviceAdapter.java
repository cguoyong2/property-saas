package com.yongquan.propertysaas.device.adapter;

import com.yongquan.propertysaas.device.domain.AccessPermissionView;
import com.yongquan.propertysaas.device.domain.DeviceConfigView;

public interface DeviceAdapter {

    AdapterResult syncAccessPermission(DeviceConfigView device, AccessPermissionView permission, String requestBody);
}
