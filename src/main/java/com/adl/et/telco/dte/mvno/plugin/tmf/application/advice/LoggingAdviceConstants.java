package com.adl.et.telco.dte.mvno.plugin.tmf.application.advice;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingAdviceConstants {

    public static final String API_NAME = "API-Name";
    public static final String CLASS_NAME = "Class-Name";
    public static final String METHOD_NAME = "Method-Name";
    public static final String PACKAGE_ROOT = "com.adl.et.telco.mvno";
    public static final String REQUEST_INITIATED = "REQUEST_INITIATED | REQUEST_METHOD : {} | REQUEST_URI : {}";
    public static final String FULL_REQUEST = "FULL_REQUEST | CONTROLLER_REQUEST : {}";
    public static final String FULL_RESPONSE = "FULL_RESPONSE | CONTROLLER_RESPONSE : {}";
    public static final String REQUEST_TERMINATED = "REQUEST_TERMINATED | RESPONSE_CODE : {} | HTTP_STATUS : {} | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_REQUEST_TERMINATED = "EXCEPTION | REQUEST_TERMINATED | ERROR_MESSAGE : {} | ERROR_CODE : {} | ERROR_STACKTRACE : {} | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_STACKTRACE = "EXCEPTION | STACKTRACE : {}";
    public static final String SERVICE_INITIATED = "SERVICE_INITIATED | REQUEST_ARGS : {}";
    public static final String SERVICE_TERMINATED = "SERVICE_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String SERVICE_TERMINATED_INFO = "SERVICE_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_SERVICE_TERMINATED = "EXCEPTION | SERVICE_TERMINATED | ERROR_MESSAGE : {} | ERROR_CODE : {} | RESPONSE_TIME : {} ms";
    public static final String MONGO_REPOSITORY_INITIATED = "MONGO_REPOSITORY_INITIATED | REQUEST_ARGS : {}";
    public static final String MONGO_REPOSITORY_TERMINATED = "MONGO_REPOSITORY_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String MONGO_REPOSITORY_TERMINATED_INFO = "MONGO_REPOSITORY_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_MONGO_REPOSITORY_TERMINATED = "EXCEPTION | MONGO_REPOSITORY_TERMINATED | ERROR_MESSAGE : {} | ERROR_CODE : {} | RESPONSE_TIME : {} ms";
    public static final String WEB_CLIENT_INITIATED = "WEB_CLIENT_INITIATED | REQUEST_ARGS : {}";
    public static final String WEB_CLIENT_TERMINATED = "WEB_CLIENT_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String WEB_CLIENT_TERMINATED_INFO = "WEB_CLIENT_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_WEB_CLIENT_TERMINATED = "EXCEPTION | WEB_CLIENT_TERMINATED | ERROR_MESSAGE : {} | ERROR_CODE : {} | RESPONSE_TIME : {} ms";
    public static final String REST_CLIENT_INITIATED = "REST_CLIENT_INITIATED | REQUEST_ARGS : {}";
    public static final String REST_CLIENT_TERMINATED = "REST_CLIENT_TERMINATED | RESPONSE_BODY : {} | RESPONSE_TIME : {} ms";
    public static final String REST_CLIENT_TERMINATED_INFO = "REST_CLIENT_TERMINATED | RESPONSE_TIME : {} ms";
    public static final String EXCEPTION_REST_CLIENT_TERMINATED = "EXCEPTION | REST_CLIENT_TERMINATED | ERROR_MESSAGE : {} | ERROR_CODE : {} | RESPONSE_TIME : {} ms";
}
