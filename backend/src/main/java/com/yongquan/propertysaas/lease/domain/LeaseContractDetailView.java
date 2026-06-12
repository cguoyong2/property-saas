package com.yongquan.propertysaas.lease.domain;

import com.yongquan.propertysaas.fee.domain.FeeBillView;
import java.util.List;

public record LeaseContractDetailView(
        LeaseContractView contract,
        List<FeeBillView> rentBills
) {
}
