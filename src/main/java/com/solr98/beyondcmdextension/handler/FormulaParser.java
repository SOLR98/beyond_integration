package com.solr98.beyondcmdextension.handler;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单的数学公式解析器
 * 支持基本算术运算和变量替换
 */
public class FormulaParser {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 支持的运算符优先级
    private static final Map<Character, Integer> OPERATOR_PRECEDENCE = new HashMap<>();
    
    static {
        OPERATOR_PRECEDENCE.put('+', 1);
        OPERATOR_PRECEDENCE.put('-', 1);
        OPERATOR_PRECEDENCE.put('*', 2);
        OPERATOR_PRECEDENCE.put('/', 2);
        OPERATOR_PRECEDENCE.put('^', 3);
    }
    
    /**
     * 计算公式值
     * @param formula 公式字符串
     * @param variables 变量映射
     * @return 计算结果
     */
    public static double evaluate(String formula, Map<String, Double> variables) {
        if (formula == null || formula.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // 替换变量
            String expression = replaceVariables(formula, variables);
            
            // 移除空格
            expression = expression.replaceAll("\\s+", "");
            
            // 简化数学符号
            expression = simplifyMathSymbols(expression);
            
            // 解析并计算
            return evaluateExpression(expression);
        } catch (Exception e) {
            LOGGER.error("Error evaluating formula '{}': {}", formula, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 替换公式中的变量
     */
    private static String replaceVariables(String formula, Map<String, Double> variables) {
        String result = formula;
        
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            String varName = entry.getKey();
            Double value = entry.getValue();
            
            // 使用正则表达式匹配变量名（避免替换部分变量名）
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(varName) + "\\b");
            Matcher matcher = pattern.matcher(result);
            
            if (matcher.find()) {
                result = matcher.replaceAll(value.toString());
            }
        }
        
        return result;
    }
    
    /**
     * 简化数学符号
     */
    private static String simplifyMathSymbols(String expression) {
        String result = expression;
        
        // 替换常见的数学符号
        result = result.replaceAll("×", "*");
        result = result.replaceAll("÷", "/");
        result = result.replaceAll("·", "*");
        result = result.replaceAll("−", "-");
        
        // 替换函数名简写
        result = result.replaceAll("\\bsqrt\\b", "sqrt");
        result = result.replaceAll("\\babs\\b", "abs");
        result = result.replaceAll("\\blog\\b", "log");
        result = result.replaceAll("\\blog10\\b", "log10");
        result = result.replaceAll("\\bsin\\b", "sin");
        result = result.replaceAll("\\bcos\\b", "cos");
        result = result.replaceAll("\\btan\\b", "tan");
        result = result.replaceAll("\\bmin\\b", "min");
        result = result.replaceAll("\\bmax\\b", "max");
        result = result.replaceAll("\\bround\\b", "round");
        result = result.replaceAll("\\bceil\\b", "ceil");
        result = result.replaceAll("\\bfloor\\b", "floor");
        
        // 替换常量简写
        result = result.replaceAll("\\bπ\\b", "pi");
        result = result.replaceAll("\\bPI\\b", "pi");
        result = result.replaceAll("\\bPi\\b", "pi");
        
        return result;
    }
    
    /**
     * 计算表达式（使用递归下降解析）
     */
    private static double evaluateExpression(String expression) {
        return parseExpression(expression, 0);
    }
    
    /**
     * 解析表达式
     */
    private static double parseExpression(String expr, int start) {
        double result = parseTerm(expr, start);
        
        int i = start;
        while (i < expr.length()) {
            char op = expr.charAt(i);
            if (op == '+' || op == '-') {
                double nextTerm = parseTerm(expr, i + 1);
                if (op == '+') {
                    result += nextTerm;
                } else {
                    result -= nextTerm;
                }
                i = skipTerm(expr, i + 1);
            } else {
                i++;
            }
        }
        
        return result;
    }
    
    /**
     * 解析项（乘除运算）
     */
    private static double parseTerm(String expr, int start) {
        double result = parseFactor(expr, start);
        
        int i = start;
        while (i < expr.length()) {
            char op = expr.charAt(i);
            if (op == '*' || op == '/') {
                double nextFactor = parseFactor(expr, i + 1);
                if (op == '*') {
                    result *= nextFactor;
                } else {
                    if (nextFactor == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result /= nextFactor;
                }
                i = skipFactor(expr, i + 1);
            } else {
                i++;
            }
        }
        
        return result;
    }
    
    /**
     * 解析因子（数字、括号、幂运算）
     */
    private static double parseFactor(String expr, int start) {
        if (start >= expr.length()) {
            return 0;
        }
        
        char firstChar = expr.charAt(start);
        
        // 处理括号
        if (firstChar == '(') {
            int end = findMatchingParenthesis(expr, start);
            if (end == -1) {
                throw new IllegalArgumentException("Unmatched parenthesis");
            }
            double result = parseExpression(expr.substring(start + 1, end), 0);
            
            // 检查是否有幂运算
            if (end + 1 < expr.length() && expr.charAt(end + 1) == '^') {
                double exponent = parseFactor(expr, end + 2);
                return Math.pow(result, exponent);
            }
            
            return result;
        }
        
        // 处理数字
        if (Character.isDigit(firstChar) || firstChar == '.') {
            int end = start;
            while (end < expr.length() && 
                   (Character.isDigit(expr.charAt(end)) || expr.charAt(end) == '.')) {
                end++;
            }
            
            double number = Double.parseDouble(expr.substring(start, end));
            
            // 检查是否有幂运算
            if (end < expr.length() && expr.charAt(end) == '^') {
                double exponent = parseFactor(expr, end + 1);
                return Math.pow(number, exponent);
            }
            
            return number;
        }
        
        // 处理负号
        if (firstChar == '-') {
            return -parseFactor(expr, start + 1);
        }
        
        // 处理函数（简单支持）
        if (Character.isLetter(firstChar)) {
            int end = start;
            while (end < expr.length() && Character.isLetter(expr.charAt(end))) {
                end++;
            }
            
            String funcName = expr.substring(start, end);
            
            // 检查是否有参数
            if (end < expr.length() && expr.charAt(end) == '(') {
                int parenEnd = findMatchingParenthesis(expr, end);
                if (parenEnd == -1) {
                    throw new IllegalArgumentException("Unmatched parenthesis for function");
                }
                
                String argStr = expr.substring(end + 1, parenEnd);
                double arg = evaluateExpression(argStr);
                
                // 支持简单函数
                switch (funcName.toLowerCase()) {
                    case "sqrt":
                        return Math.sqrt(arg);
                    case "abs":
                        return Math.abs(arg);
                    case "log":
                        return Math.log(arg);
                    case "log10":
                        return Math.log10(arg);
                    case "sin":
                        return Math.sin(Math.toRadians(arg));
                    case "cos":
                        return Math.cos(Math.toRadians(arg));
                    case "tan":
                        return Math.tan(Math.toRadians(arg));
                    case "min":
                        // 需要两个参数，这里简单处理
                        return Math.min(arg, 0);
                    case "max":
                        return Math.max(arg, 0);
                    case "round":
                        return Math.round(arg);
                    case "ceil":
                        return Math.ceil(arg);
                    case "floor":
                        return Math.floor(arg);
                    default:
                        throw new IllegalArgumentException("Unknown function: " + funcName);
                }
            } else {
                // 可能是常量
                switch (funcName.toLowerCase()) {
                    case "pi":
                        return Math.PI;
                    case "e":
                        return Math.E;
                    default:
                        throw new IllegalArgumentException("Unknown constant: " + funcName);
                }
            }
        }
        
        throw new IllegalArgumentException("Invalid character at position " + start + ": " + firstChar);
    }
    
    /**
     * 跳过项
     */
    private static int skipTerm(String expr, int start) {
        return skipFactor(expr, start);
    }
    
    /**
     * 跳过因子
     */
    private static int skipFactor(String expr, int start) {
        if (start >= expr.length()) {
            return start;
        }
        
        char firstChar = expr.charAt(start);
        
        if (firstChar == '(') {
            int end = findMatchingParenthesis(expr, start);
            if (end == -1) {
                return expr.length();
            }
            return end + 1;
        }
        
        if (Character.isDigit(firstChar) || firstChar == '.') {
            int end = start;
            while (end < expr.length() && 
                   (Character.isDigit(expr.charAt(end)) || expr.charAt(end) == '.')) {
                end++;
            }
            return end;
        }
        
        if (firstChar == '-') {
            return skipFactor(expr, start + 1);
        }
        
        if (Character.isLetter(firstChar)) {
            int end = start;
            while (end < expr.length() && Character.isLetter(expr.charAt(end))) {
                end++;
            }
            
            if (end < expr.length() && expr.charAt(end) == '(') {
                int parenEnd = findMatchingParenthesis(expr, end);
                if (parenEnd == -1) {
                    return expr.length();
                }
                return parenEnd + 1;
            }
            
            return end;
        }
        
        return start + 1;
    }
    
    /**
     * 查找匹配的括号
     */
    private static int findMatchingParenthesis(String expr, int start) {
        if (expr.charAt(start) != '(') {
            return -1;
        }
        
        int depth = 1;
        for (int i = start + 1; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * 验证公式语法
     */
    public static boolean validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 测试公式是否有效
            Map<String, Double> testVars = new HashMap<>();
            testVars.put("base", 1.0);
            testVars.put("level", 1.0);
            testVars.put("multiplier", 1.0);
            testVars.put("books", 1.0);
            
            evaluate(formula, testVars);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Formula validation failed for '{}': {}", formula, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取公式中的变量列表
     */
    public static String[] extractVariables(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            return new String[0];
        }
        
        // 简单提取单词（变量名通常由字母组成）
        Pattern pattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
        Matcher matcher = pattern.matcher(formula);
        
        java.util.Set<String> variables = new java.util.HashSet<>();
        while (matcher.find()) {
            String var = matcher.group();
            
            // 排除已知函数和常量
            if (!isFunction(var) && !isConstant(var)) {
                variables.add(var);
            }
        }
        
        return variables.toArray(new String[0]);
    }
    
    private static boolean isFunction(String name) {
        String[] functions = {"sqrt", "abs", "log", "log10", "sin", "cos", "tan", 
                              "min", "max", "round", "ceil", "floor"};
        for (String func : functions) {
            if (func.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isConstant(String name) {
        String[] constants = {"pi", "e"};
        for (String constant : constants) {
            if (constant.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}