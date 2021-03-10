package com.farzadz.optimizer.core;

import static com.farzadz.optimizer.core.STATE.LTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IPSolverTest {

  @Test
  public void testSingleVariableMax1() {
    IPSolver solver = new IPSolver();
    String food = "burger";
    solver.bind(food, 0, 2, food);
    Map<Object, Long> objectValueMap = solver.solveMaximise(List.of(food), List.of(1));
    assertEquals(2L, objectValueMap.get(food));
  }

  @Test
  public void testSingleVariableMax2() {
    IPSolver solver = new IPSolver();
    String food = "burger";
    solver.bind(food, 0, 2, food);
    Map<Object, Long> objectValueMapNegativeObjective = solver.solveMaximise(List.of(food), List.of(-1));
    assertEquals(0L, objectValueMapNegativeObjective.get(food));
  }

  @Test
  public void testSingleVariableMin1() {
    IPSolver solver = new IPSolver();
    String food = "burger";
    solver.bind(food, 0, 1, food);
    Map<Object, Long> objectValueMap = solver.solveMinimize(List.of(food), List.of(1));
    assertEquals(0L, objectValueMap.get(food));
  }

  @Test
  public void testSingleVariableMin2() {
    IPSolver solver = new IPSolver();
    String food = "burger";
    solver.bind(food, 0, 1, food);

    Map<Object, Long> objectValueMapNegativeObjective = solver.solveMinimize(List.of(food), List.of(-1));
    assertEquals(1L, objectValueMapNegativeObjective.get(food));
  }

  @Test
  public void testSingleVariableMinEnforceValue() {
    IPSolver solver = new IPSolver();
    String food = "burger";
    solver.bind(food, 0, 1, food);
    solver.enforceValue(food, 1);
    Map<Object, Long> objectValueMap = solver.solveMinimize(List.of(food), List.of(1));
    assertEquals(1L, objectValueMap.get(food));
  }

  @Test
  public void testSolveWithCondition1() {
    IPSolver solver = new IPSolver();
    String burger = "burger";
    String pizza = "pizza";
    String coke = "coke";
    String juice = "juice";
    solver.bind(burger, 0, 1, burger);
    solver.bind(pizza, 0, 1, pizza);
    solver.bind(coke, 0, 1, coke);
    solver.bind(juice, 0, 1, juice);
    solver.enforceValue(burger, 1);
    solver.ifThen(List.of(burger, coke), List.of(2, 3), 2L, STATE.GTE, List.of(juice), List.of(2), 2L, STATE.GTE, "");
    solver.ifThen(List.of(juice), List.of(1), 1L, STATE.GTE, List.of(pizza), List.of(1), 0L, STATE.GTE, "");

    Map<Object, Long> objectValueMapMax = solver
        .solveMaximise(List.of(burger, coke, juice, pizza), List.of(1, 1, 1, 1));
    assertEquals(1, objectValueMapMax.get(burger));
    assertEquals(1, objectValueMapMax.get(juice));
    assertEquals(1, objectValueMapMax.get(coke));
    assertEquals(1, objectValueMapMax.get(pizza));
  }

  @Test
  public void testSolveWithCondition2() {
    IPSolver solver = new IPSolver();
    String burger = "burger";
    String pizza = "pizza";
    String coke = "coke";
    String juice = "juice";
    solver.bind(burger, 0, 1, burger);
    solver.bind(pizza, 0, 1, pizza);
    solver.bind(coke, 0, 1, coke);
    solver.bind(juice, 0, 1, juice);
    solver.enforceValue(burger, 1);
    solver.ifThen(List.of(burger, coke), List.of(2, 3), 2L, STATE.GTE, List.of(juice), List.of(2), 2L, STATE.GTE, "");
    solver.ifThen(List.of(juice), List.of(1), 1L, STATE.GTE, List.of(pizza), List.of(1), 0L, STATE.GTE, "");
    Map<Object, Long> objectValueMapMin = solver
        .solveMinimize(List.of(burger, coke, juice, pizza), List.of(1, 1, 1, 1));
    assertEquals(1, objectValueMapMin.get(burger));
    assertEquals(1, objectValueMapMin.get(juice));
    assertEquals(0, objectValueMapMin.get(pizza));
    assertEquals(0, objectValueMapMin.get(coke));
  }

  @Test
  public void testEnforceValueMinimise() {
    IPSolver solver = new IPSolver();
    Object variable = new Object();
    String description = "Single Variable";
    solver.bind(variable, 0, 2, description);
    solver.enforceValue(variable, 1);

    Map<Object, Long> objectBooleanMap = solver.solveMinimize(List.of(variable), List.of(1));
    assertEquals(1, objectBooleanMap.get(variable));
  }

  @Test
  public void testEnforceValueMaximise() {
    IPSolver solver = new IPSolver();
    Object variable = new Object();
    String description = "Single Variable";
    solver.bind(variable, 0, 2, description);
    solver.enforceValue(variable, 1);

    Map<Object, Long> objectBooleanMap = solver.solveMaximise(List.of(variable), List.of(1));
    assertEquals(1, objectBooleanMap.get(variable));
  }

  @Test
  public void testEnforceValueWithOutboundEnforcementMinimise() {
    IPSolver solver = new IPSolver();
    Object variable = new Object();
    String description = "Single Variable";
    solver.bind(variable, 0, 2, description);
    solver.enforceValue(variable, 3);

    assertThrows(IllegalStateException.class, () -> solver.solveMinimize(List.of(variable), List.of(1)));
  }

  @Test
  public void testEnforceValueWithOutboundEnforcementMaximise() {
    IPSolver solver = new IPSolver();
    Object variable = new Object();
    String description = "Single Variable";
    solver.bind(variable, 0, 2, description);
    solver.enforceValue(variable, -1);

    assertThrows(IllegalStateException.class, () -> solver.solveMinimize(List.of(variable), List.of(1)));
  }

  /**
   * From https://developers.google.com/optimization/cp/integer_opt_cp
   * Maximize 2x + 2y + 3z subject to the following constraints:
   * x + 7/2 y + 3/2z	≤	25
   * 3x - 5y + 7z	≤	45
   * 5x + 2y - 6z	≤	37
   * x, y, z	≥	0
   * x, y, z integers
   */
  @Test
  public void testIP() {
    IPSolver solver = new IPSolver();
    String x = "X";
    String y = "Y";
    String z = "Z";
    solver.bind(x, 0, Integer.MAX_VALUE, x);
    solver.bind(y, 0, Integer.MAX_VALUE, y);
    solver.bind(z, 0, Integer.MAX_VALUE, z);
    solver.enforceExpressionRelationship(List.of(x, y, z), List.of(2, 7, 3), 50, LTE, "first constraint");
    solver.enforceExpressionRelationship(List.of(x, y, z), List.of(3, -5, 7), 45, LTE, "second constraint");
    solver.enforceExpressionRelationship(List.of(x, y, z), List.of(5, 2, -6), 37, LTE, "third constraint");
    Map<Object, Long> objectValueMap = solver.solveMaximise(List.of(x, y, z), List.of(2, 2, 3));

    assertEquals(7, objectValueMap.get(x));
    assertEquals(3, objectValueMap.get(y));
    assertEquals(5, objectValueMap.get(z));

  }
}