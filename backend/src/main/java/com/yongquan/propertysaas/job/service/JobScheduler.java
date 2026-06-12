package com.yongquan.propertysaas.job.service;

import com.yongquan.propertysaas.job.config.JobProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobOrchestrationService service;
    private final JobProperties properties;

    public JobScheduler(JobOrchestrationService service, JobProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(cron = "${property-saas.job.work-order-sla-cron:0 */10 * * * *}")
    public void workOrderSla() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Run scheduled job {}", JobOrchestrationService.JOB_WORK_ORDER_SLA);
        service.runWorkOrderSlaForAllTenants(properties.getDefaultLimit());
    }

    @Scheduled(cron = "${property-saas.job.patrol-missed-cron:0 */15 * * * *}")
    public void patrolMissed() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Run scheduled job {}", JobOrchestrationService.JOB_PATROL_MISSED);
        service.runPatrolMissedForAllTenants(properties.getDefaultLimit());
    }

    @Scheduled(cron = "${property-saas.job.lease-expire-cron:0 0 9 * * *}")
    public void leaseExpireRemind() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Run scheduled job {}", JobOrchestrationService.JOB_LEASE_EXPIRE_REMIND);
        service.runLeaseExpireRemindForAllTenants(properties.getLeaseExpireDays(), properties.getDefaultLimit());
    }

    @Scheduled(cron = "${property-saas.job.message-dispatch-cron:0 */2 * * * *}")
    public void messageDispatch() {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Run scheduled job {}", JobOrchestrationService.JOB_MESSAGE_DISPATCH);
        service.runMessageDispatchForAllTenants(properties.getDefaultLimit());
    }
}
