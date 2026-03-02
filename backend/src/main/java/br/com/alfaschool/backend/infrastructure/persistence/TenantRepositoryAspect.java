package br.com.alfaschool.backend.infrastructure.persistence;

import br.com.alfaschool.backend.security.filter.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class TenantRepositoryAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* br.com.alfaschool.backend.infrastructure.persistence.repository..*(..))")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return joinPoint.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenantId);
        try {
            return joinPoint.proceed();
        } finally {
            session.disableFilter("tenantFilter");
        }
    }
}
