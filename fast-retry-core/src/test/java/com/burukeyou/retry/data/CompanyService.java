package com.burukeyou.retry.data;

import com.burukeyou.retry.core.annotations.FastRetry;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @FastRetry
    public String getCompany(String name) {
        int i = 1 / 0;
       return name + "_not_found";
    }

    @CustomFastRetry(maxAttempts = 2)
    public String getCompanyByCustom(String name) {
        System.out.println("aaaa");
        return "company";
    }

}
