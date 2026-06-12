package com.yongquan.propertysaas;

import com.yongquan.propertysaas.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

class PropertySaasApplicationTests {

    @Test
    void apiResponseUsesSuccessCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        org.assertj.core.api.Assertions.assertThat(response.code()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.data()).isEqualTo("ok");
    }
}
