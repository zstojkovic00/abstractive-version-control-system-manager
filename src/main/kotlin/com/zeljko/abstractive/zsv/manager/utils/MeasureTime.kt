package com.zeljko.abstractive.zsv.manager.utils

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MeasureTime


@Aspect
@Component
class MeasureTimeAspect {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Around("@annotation(MeasureTime)")
    fun measureExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val stopWatch = StopWatch(joinPoint.signature.name)

        return try {
            stopWatch.start()
            val result = joinPoint.proceed()
            stopWatch.stop()

            logger.info(stopWatch.prettyPrint())
            result
        } catch (e: Throwable) {
            stopWatch.stop()
            throw e
        }
    }
}
