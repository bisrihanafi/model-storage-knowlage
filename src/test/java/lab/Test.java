/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package lab;

/**
 * Bisri Hanafi bisrihanafi@designjaya.com
 *
 * Jul 27, 2026 2:30:49 PM
 *
 * @author HP
 */
public class Test {

    public static void main(String[] args) throws Exception {
        test1();
    }

    private static void test1() throws Exception {
        try {
            Integer.parseInt("");
            throw new Exception();
        } catch (RuntimeException er) {
            System.out.println("KENA");
            er.printStackTrace();
        }
    }
}
