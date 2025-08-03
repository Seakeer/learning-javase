package me.seakeer.learning.javase.exceptionhandle;

import java.io.IOException;

/**
 * ExceptionHandleExample;
 * 异常及其处理机制示例
 *
 * @author Seakeer;
 * @date 2024/8/31;
 */
public class ExceptionHandleExample {
    public static void main(String[] args) {
        try {
            ExceptionHandleExample.triggerMyException();
        } catch (MyException e) {
            e.printExceptionType();
            new ThrowCatchException().method5();
        } finally {
            System.out.println("Finally Executed.");
        }
    }

    public static void triggerMyException() throws MyException {
        //方法体内抛出异常
        throw new MyException();
    }
}

/**
 * 自定义异常
 */
class MyException extends Exception {
    public MyException() {
        // 调用父类无参构造方法
        super();
    }

    public void printExceptionType() {
        System.out.println("My Exception");
    }
}

/**
 * 异常抛出及捕获
 */
class ThrowCatchException {
    // 方法声明异常
    void method1() throws IOException {
        System.out.println("Throw Exception");
    }

    // 自己无法处理异常，则声明抛出IOException
    void method2() throws IOException {
        method1();
    }

    // 自己无法处理异常，声明抛出父类Exception
    void method3() throws Exception {
        method1();
    }

    // 自己可以处理异常，进行捕获IOException
    void method4() {
        try {
            method1();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Finally Executed.");
        }
    }

    // 自己可以处理异常，捕获Exception
    void method5() {
        try {
            method1();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 自己可以处理部分异常，声明抛出Exception
    void method6() throws Exception {
        try {
            method1();
        } catch (IOException e) {
            throw new Exception();
        }
    }
  
 /*
  // 编译错误
	void method2(){    
		method1();    
	}    
	// 编译错误，必须捕获或声明抛出Exception    
	void method6(){    
		try{    
			method1();    
		}catch(IOException e){
			throw new Exception();
		}    
	}    
  */
}