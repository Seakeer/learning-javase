package me.seakeer.learning.javase.other.logging.sl4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Slf4jExample;
 *
 * @author Seakeer;
 * @date 2025/7/11;
 */
public class Slf4jExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jExample.class);

    public static void main(String[] args) {
        try {
            MDC.put("TRACE_ID", "MDC_EXAMPLE");
            LOGGER.info("SLF4J EXAMPLE");
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            new Thread(() -> {
                MDC.setContextMap(mdcContext);
                LOGGER.trace("TRACE LOG");
                LOGGER.debug("DEBUG LOG");
                LOGGER.info("INFO LOG");
                LOGGER.warn("WARN LOG");
                LOGGER.error("ERROR LOG");
            }).start();
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            MDC.clear();
        }
    }
}
