package br.com.alfaschool.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Habilita agendamento e execucao assincrona, que o modulo de acesso usa
 * para heartbeat de SSE, fila de notificacoes, expiracao de dispositivo
 * offline e recalculo de permanencia.
 *
 * O scheduler tem mais de uma thread de proposito: com pool de 1, uma
 * tarefa lenta (varredura da fila de e-mail) atrasa o heartbeat dos
 * paineis e as TVs comecam a cair sozinhas.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class AsyncSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("alfaschool-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }

    @Bean(name = "notificacaoExecutor")
    public Executor notificacaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        // Fila limitada: no pico de saida centenas de avisos saem juntos e
        // uma fila ilimitada viraria acumulo de memoria silencioso.
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("alfaschool-notif-");
        executor.initialize();
        return executor;
    }
}
