package com.adl.et.telco.dte.mvno.plugin.tmf.application.advice;

import ch.qos.logback.classic.Logger;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.BaseException;
import com.adl.et.telco.dte.plugin.logging.services.LoggingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Component
@Aspect
public class LoggingAdvice {

    private static final Logger log = LoggingUtils.getLogger(LoggingAdvice.class.getName());

    private final ObjectMapper objectMapper;

    public LoggingAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) && args(.., request)")
    public Object logGetMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) && args(@RequestBody body,.., request)")
    public Object logPostMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request, Object body) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.PatchMapping) && args(@RequestBody body,.., request)")
    public Object logPatchMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request, Object body) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("@annotation(org.springframework.web.bind.annotation.DeleteMapping) && args(.., request)")
    public Object logDeleteMethod(ProceedingJoinPoint jointPoint, HttpServletRequest request) {
        return logRequest(jointPoint, request);
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.mvno.*.domain.service..*(..)) || execution(* com.adl.et.telco.dte.mvno.plugin.tmf.domain.service..*(..))")
    public Object logServiceInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.SERVICE_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            }
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.SERVICE_TERMINATED_INFO, elapsedTime);
            if (log.isDebugEnabled()) {
                String response = objectMapper.writeValueAsString(object);
                log.debug(LoggingAdviceConstants.SERVICE_TERMINATED, response, elapsedTime);
            }
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_SERVICE_TERMINATED, ex.getMessage(), ex.getCode(), elapsedTime);
            throw ex;
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_SERVICE_TERMINATED, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), elapsedTime);
            throw ex;
        }
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.mvno.*.external.repositories..*(..)) || execution(* com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories..*(..))")
    public Object logRepositoryInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.MONGO_REPOSITORY_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            }
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.MONGO_REPOSITORY_TERMINATED_INFO, elapsedTime);
            if (log.isDebugEnabled()) {
                String response = objectMapper.writeValueAsString(object);
                log.debug(LoggingAdviceConstants.MONGO_REPOSITORY_TERMINATED, response, elapsedTime);
            }
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_MONGO_REPOSITORY_TERMINATED, ex.getMessage(), ex.getCode(), elapsedTime);
            throw ex;
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_MONGO_REPOSITORY_TERMINATED, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), elapsedTime);
            throw ex;
        }
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.mvno.*.external.webclients.*.*(..)))")
    public Object logWebclientInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.WEB_CLIENT_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            }
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.WEB_CLIENT_TERMINATED_INFO, elapsedTime);
            if (log.isDebugEnabled()) {
                String response = objectMapper.writeValueAsString(object);
                log.debug(LoggingAdviceConstants.WEB_CLIENT_TERMINATED, response, elapsedTime);
            }
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_WEB_CLIENT_TERMINATED, ex.getMessage(), ex.getCode(), elapsedTime);
            throw ex;
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_WEB_CLIENT_TERMINATED, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), elapsedTime);
            throw ex;
        }
    }

    @SneakyThrows
    @Around("execution(* com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients.util..*(..)))")
    public Object logRestClientInfo(ProceedingJoinPoint proceedingJoinPoint) {
        String className = null;
        String methodName = null;
        long start = System.currentTimeMillis();
        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            className = methodSignature.getDeclaringTypeName();
            methodName = methodSignature.getName();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.REST_CLIENT_INITIATED, Arrays.toString(proceedingJoinPoint.getArgs()));
            }
            Object object = proceedingJoinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.REST_CLIENT_TERMINATED_INFO, elapsedTime);
            if (log.isDebugEnabled()) {
                String response = objectMapper.writeValueAsString(object);
                log.debug(LoggingAdviceConstants.REST_CLIENT_TERMINATED, response, elapsedTime);
            }
            return object;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REST_CLIENT_TERMINATED, ex.getMessage(), ex.getCode(), elapsedTime);
            throw ex;
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REST_CLIENT_TERMINATED, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), elapsedTime);
            throw ex;
        }
    }

    @SneakyThrows
    private Object logRequest(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringTypeName();
        String methodName = methodSignature.getName();
        long start = System.currentTimeMillis();
        MDC.put(LoggingAdviceConstants.API_NAME, methodName);
        MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
        MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
        log.info(LoggingAdviceConstants.REQUEST_INITIATED, request.getMethod(), request.getRequestURI());
        if (log.isDebugEnabled()) {
            log.debug(LoggingAdviceConstants.FULL_REQUEST, Arrays.toString(joinPoint.getArgs()));
        }
        return logResponseAndHeader(joinPoint, start, className, methodName);
    }

    @SneakyThrows
    public Object logResponseAndHeader(ProceedingJoinPoint joinPoint, long start, String className, String methodName) {
        ResponseEntity<?> result;
        try {
            result = (ResponseEntity<?>) joinPoint.proceed();
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            String resultCode = result.getStatusCode().toString();
            int statusCodeValue = result.getStatusCode().value();
            long elapsedTime = System.currentTimeMillis() - start;
            log.info(LoggingAdviceConstants.REQUEST_TERMINATED, resultCode, statusCodeValue, elapsedTime);
            if (log.isDebugEnabled()) {
                String response = objectMapper.writeValueAsString(result);
                log.debug(LoggingAdviceConstants.FULL_RESPONSE, response);
            }
            return result;
        } catch (BaseException ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REQUEST_TERMINATED, ex.getMessage(), ex.getCode(), displayStackStraceArray(ex.getStackTrace()), elapsedTime);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.EXCEPTION_STACKTRACE, Arrays.toString(ex.getStackTrace()));
            }
            throw ex;
        } catch (Exception ex) {
            MDC.put(LoggingAdviceConstants.CLASS_NAME, className);
            MDC.put(LoggingAdviceConstants.METHOD_NAME, methodName);
            long elapsedTime = System.currentTimeMillis() - start;
            log.error(LoggingAdviceConstants.EXCEPTION_REQUEST_TERMINATED, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), displayStackStraceArray(ex.getStackTrace()), elapsedTime);
            if (log.isDebugEnabled()) {
                log.debug(LoggingAdviceConstants.EXCEPTION_STACKTRACE, Arrays.toString(ex.getStackTrace()));
            }
            throw ex;
        }
    }

    public String displayStackStraceArray(StackTraceElement[] stackTraceElements) {
        StringBuilder stringBuilder = new StringBuilder();
        if (stackTraceElements != null) {
            for (StackTraceElement elem : stackTraceElements) {
                if (elem.getClassName().startsWith(LoggingAdviceConstants.PACKAGE_ROOT) && elem.getLineNumber() > 0) {
                    stringBuilder.append(elem);
                    break;
                }
            }
        }
        return stringBuilder.toString();
    }

}
