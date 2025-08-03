package me.seakeer.learning.javase.other.logging.jdklog;

import java.io.IOException;
import java.util.logging.*;

/**
 * LoggerFactory;
 *
 * @author Seakeer;
 * @date 2025/7/10;
 */
public class LoggerFactory {

    public static Logger getLoggerFromConfigFile(String loggerName) {

        try {
            LogManager.getLogManager().readConfiguration(LoggerFactory.class.getClassLoader().getResourceAsStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Logger.getLogger(loggerName);
    }

    public static Logger getLoggerFromConfigClass(String loggerName) {

        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(Level.INFO);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(consoleHandler);

        try {
            FileHandler fileHandler = new FileHandler("learning-javalang.log",
                    50000, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logger;
    }
}
