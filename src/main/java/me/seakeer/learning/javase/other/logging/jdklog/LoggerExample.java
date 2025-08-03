package me.seakeer.learning.javase.other.logging.jdklog;

import java.util.logging.Logger;

/**
 * LoggerExample;
 *
 * @author Seakeer;
 * @date 2025/1/4;
 */
public class LoggerExample {
    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLoggerFromConfigClass(LoggerExample.class.getName());
        logger.finest("JDK LOG FINEST");
        logger.finer("JDK LOG FINER");
        logger.fine("JDK LOG FINE");
        logger.config("JDK LOG CONFIG");
        logger.info("JDK LOG INFO");
        logger.warning("JDK LOG WARNING");
        logger.severe("JDK LOG SEVERE");

        logger = LoggerFactory.getLoggerFromConfigFile(LoggerExample.class.getName());
        logger.finest("JDK LOG FINEST");
        logger.finer("JDK LOG FINER");
        logger.fine("JDK LOG FINE");
        logger.config("JDK LOG CONFIG");
        logger.info("JDK LOG INFO");
        logger.warning("JDK LOG WARNING");
        logger.severe("JDK LOG SEVERE");
    }
}
