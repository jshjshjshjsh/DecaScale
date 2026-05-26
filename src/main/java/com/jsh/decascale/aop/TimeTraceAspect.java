package com.jsh.decascale.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class TimeTraceAspect {

    // 🎯 타겟 설정: controller 패키지 하위에 있는 모든 클래스의 모든 메서드에 적용
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    private void controllerPointcut() {}

    // ⏱️ 실제 실행 로직: 타겟 메서드를 감싸서(Around) 앞뒤로 시간 측정
    @Around("controllerPointcut()")
    public Object traceTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        //log.info("[API Start] {}", joinPoint.getSignature().toShortString());

        try {
            // 실제 컨트롤러 메서드 실행 (이때 쿼리도 돌고 서비스 로직도 돌아감)
            return joinPoint.proceed();
        } finally {
            long finish = System.currentTimeMillis();
            long timeMs = finish - start;
            double timeSeconds = timeMs / 1000.0;

            // 소수점 셋째 자리(밀리초)까지 예쁘게 잘라서 출력
            log.info("[API End] {} -> 실행 시간: {}s",
                    joinPoint.getSignature().toShortString(),
                    String.format("%.3f", timeSeconds));
        }
    }
}