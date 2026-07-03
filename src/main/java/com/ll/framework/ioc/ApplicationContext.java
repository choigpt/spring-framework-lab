package com.ll.framework.ioc;

import com.ll.domain.testPost.testPost.repository.TestPostRepository;
import com.ll.domain.testPost.testPost.service.TestFacadePostService;
import com.ll.domain.testPost.testPost.service.TestPostService;


public class ApplicationContext {
    private final TestPostRepository testPostRepository;
    private final TestPostService testPostService;
    private final TestFacadePostService testFacadePostService;

    public ApplicationContext() {
        testPostRepository = new TestPostRepository();
        testPostService = new TestPostService(testPostRepository);
        testFacadePostService = new TestFacadePostService(testPostService, testPostRepository);
    }

    public <T> T genBean(String name) {
        return switch (name) {
            case "testPostRepository" -> (T) testPostRepository;
            case "testPostService" -> (T) testPostService;
            case "testFacadePostService" -> (T) testFacadePostService;
            default -> null;
        };
    }
}
