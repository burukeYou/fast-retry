package com.burukeyou.retry.core.enums;

import lombok.Getter;

/**
 * print exception log strategy
 * @author caizhihao
 */
@Getter
public enum LogEnum {

    /**
     * Automatically go through different printing logic according to the different maxAttempts values
     * <ul>
     *     <li>When it is 0, it is not printed</li>
     *     <li>When less than or equal to 2, print every time</li>
     *     <li>When greater than 2, it is equivalent to {@link #PRE_2_LAST_1}</li>
     * </ul>
     */
    AUTO,

    /**
     * Print the exception log for each time
     */
    EVERY,

    /**
     * not print the exception log
     */
    NOT,

    /**
     * Print the first N exception logs
     */
    PRE_1(1),
    PRE_2(2),
    PRE_3(3),
    PRE_4(4),
    PRE_5(5),
    PRE_6(6),
    PRE_7(7),
    PRE_8(8),
    PRE_9(9),

    /**
     * Print the first N exception logs and the last N exception logs
     */
    PRE_1_LAST_1(1,1),
    PRE_2_LAST_1(2,1),
    PRE_3_LAST_1(3,1),

    PRE_2_LAST_2(2,2),
    PRE_3_LAST_2(3,2),
    PRE_4_LAST_2(4,2),

    ;

    private Integer preN;
    private Integer lastN;

    LogEnum() {

    }

    LogEnum(Integer n) {
        this.preN = n;
    }

    LogEnum(Integer preN, Integer lastN) {
        this.preN = preN;
        this.lastN = lastN;
    }
}
