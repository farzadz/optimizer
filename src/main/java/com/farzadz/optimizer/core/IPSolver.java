package com.farzadz.optimizer.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.DecisionStrategyProto;
import com.google.ortools.sat.DecisionStrategyProto.DomainReductionStrategy;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class IPSolver {

  static {
    System.loadLibrary("jniortools");
  }

  private CpSolver solver = new CpSolver();

  private CpModel model = new CpModel();

  private CpSolverStatus solverStatus;

  private AtomicLong conditionIdGen = new AtomicLong(1);

  private final BiMap<Object, IntVar> objectVariableMap = HashBiMap.create();

  /**
   * Registers a query to solver to check if the sum of variables associated to the first argument
   * has the specified relationship or not. The variables this method adds to the solver have no impact on the
   * final status, however, they will be present in the final solution. The caller method should keep the references of
   * their variables of interest.
   *
   * @return Reference to an object whose final value can be checked after solving the model to check if the specified
   *     condition is satisfied or not.
   */
  //  protected Object expressionStatus(List<?> objects, Long l, STATE state) {
  //
  //    // auxiliary variable for keeping track of expression status
  //
  //    Object isActive = new Object();
  //    bind(isActive, String.format("Expression track variable for checking if sum of %s is %s %s", objects, state, l));
  //
  //    String conditionDescription = String.format("Condition for checking if sum of %s is %s %s", objects, state, l);
  //    ifThen(objects, l, state, List.of(isActive), 1L, EQ, conditionDescription);
  //
  //    switch (state) {
  //      case GTE:
  //        ifThen(objects, l, LT, List.of(isActive), 0L, EQ, conditionDescription);
  //        break;
  //      case LTE:
  //        ifThen(objects, l, GT, List.of(isActive), 0L, EQ, conditionDescription);
  //        break;
  //      case EQ:
  //        ifThen(objects, l, GT, List.of(isActive), 0L, EQ, conditionDescription);
  //        ifThen(objects, l, LT, List.of(isActive), 0L, EQ, conditionDescription);
  //        break;
  //      case LT:
  //        ifThen(objects, l, GTE, List.of(isActive), 0L, EQ, conditionDescription);
  //        break;
  //      case GT:
  //        ifThen(objects, l, LTE, List.of(isActive), 0L, EQ, conditionDescription);
  //        break;
  //    }
  //    return isActive;
  //  }

  /**
   * If the sum of values in ifCondition has the specified relationship with l1, then the sum of predicate
   * collection is enforced to have its own specified relationship with l2 (e.g. if at least one of the objects of the
   * first collection is in the
   * final solution, we want no object of the second collection to be present. This will enforced by:
   * ifThen(firstCollection, 1, GTE, secondCollection, 0 , EQ) ).
   *
   * @param predicateVariables
   *     Collection of already bound (with {@link IPSolver#bind(Object, String)}) objects.
   * @param l1
   *     Number to be used in (in)equality relationship with predicate
   * @param state1
   *     Relationship of Sum(predicate) with l1 ( ==, >, >= , <=, <) values defined by {@link STATE}
   * @param entailmentVariables
   *     Collection of already bound (with {@link IPSolver#bind(Object, String)}) objects.
   * @param l2
   *     Number to be used in (in)equality relationship with entailment
   * @param state2
   *     Relationship of Sum(entailment) with l1 ( ==, >, >= , <=, <) values defined by {@link STATE}
   */
  public ConditionHolder ifThen(List<?> predicateVariables, List<Integer> predicateCoefficients, Long l1, STATE state1,
      List<?> entailmentVariables, List<Integer> entailmentCoefficients, Long l2, STATE state2, String description) {

    if (predicateCoefficients.size() != predicateVariables.size()) {
      throw new IllegalArgumentException(String
          .format("Mismatch between size of predicate coefficients %d and variables %d for condition %s",
              predicateCoefficients.size(), predicateVariables.size(), description));
    }
    if (entailmentCoefficients.size() != entailmentVariables.size()) {
      throw new IllegalArgumentException(String
          .format("Mismatch between size of entailment coefficients %d and variables %d for condition %s",
              entailmentCoefficients.size(), entailmentVariables.size(), description));
    }
    predicateVariables.forEach(o -> {
      if (!objectVariableMap.containsKey(o)) {
        throw new IllegalArgumentException(
            String.format("Object %s is not known to the solver, try registering it with bind", o.toString()));
      }
    });

    entailmentVariables.forEach(o -> {
      if (!objectVariableMap.containsKey(o)) {
        throw new IllegalArgumentException(
            String.format("Object %s is not known to the solver, try registering it with bind", o.toString()));
      }
    });

    LinearExpr expr1 = LinearExpr
        .scalProd(predicateVariables.stream().map(objectVariableMap::get).toArray(IntVar[]::new),
            predicateCoefficients.stream().mapToInt(e -> e).toArray());
    LinearExpr expr2 = LinearExpr
        .scalProd(entailmentVariables.stream().map(objectVariableMap::get).toArray(IntVar[]::new),
            entailmentCoefficients.stream().mapToInt(e -> e).toArray());

    // traces what happens to the to the entailment
    //note that if the predicate is false, entailment will be free to have any relationship with l2
    ConditionHolder rightHandSideStatus = new ConditionHolder(conditionIdGen.incrementAndGet(), description);

    IntVar condition = bind(rightHandSideStatus, 0, 1, description);

    switch (state2) {
      case EQ:
        model.addEquality(expr2, l2).onlyEnforceIf(condition);
        model.addDifferent(expr2, l2).onlyEnforceIf(condition.not());
        break;
      case GTE:
        model.addGreaterOrEqual(expr2, l2).onlyEnforceIf(condition);
        model.addLessThan(expr2, l2).onlyEnforceIf(condition.not());
        break;
      case LTE:
        model.addLessOrEqual(expr2, l2).onlyEnforceIf(condition);
        model.addGreaterThan(expr2, l2).onlyEnforceIf(condition.not());
        break;
      case LT:
        model.addLessThan(expr2, l2).onlyEnforceIf(condition);
        model.addGreaterOrEqual(expr2, l2).onlyEnforceIf(condition.not());
        break;
      case GT:
        model.addGreaterThan(expr2, l2).onlyEnforceIf(condition);
        model.addLessOrEqual(expr2, l2).onlyEnforceIf(condition.not());
        break;
      default:
        throw new IllegalArgumentException();
    }

    switch (state1) {
      case EQ:
        model.addDifferent(expr1, l1).onlyEnforceIf(condition.not());
        break;
      case GTE:
        model.addLessThan(expr1, l1).onlyEnforceIf(condition.not());
        break;
      case LTE:
        model.addGreaterThan(expr1, l1).onlyEnforceIf(condition.not());
        break;
      case LT:
        model.addGreaterOrEqual(expr1, l1).onlyEnforceIf(condition.not());
        break;
      case GT:
        model.addLessOrEqual(expr1, l1).onlyEnforceIf(condition.not());
        break;
      default:
        throw new IllegalArgumentException();
    }
    return rightHandSideStatus;
  }

  public void enforceValue(Object variable, int value) {
    if (!objectVariableMap.containsKey(variable)) {
      throw new IllegalArgumentException(String.format("Object %s is unknown to the model solver", variable));
    }
    model.addEquality(objectVariableMap.get(variable), value);
  }

  public void enforceExpressionRelationship(List<?> objects, List<Integer> coefficients, int value, STATE state,
      String description) {
    if (objects.size() != coefficients.size()) {
      throw new IllegalArgumentException(String
          .format("Mismatch between size of expression variables %d and coefficients %d for %s", objects.size(),
              coefficients.size(), description));
    }

    LinearExpr expr = LinearExpr.scalProd(objects.stream().map(objectVariableMap::get).toArray(IntVar[]::new),
        coefficients.stream().mapToInt(e -> e).toArray());
    switch (state) {
      case EQ:
        model.addEquality(expr, value);
        break;
      case GTE:
        model.addGreaterOrEqual(expr, value);
        break;
      case GT:
        model.addGreaterThan(expr, value);
        break;
      case LTE:
        model.addLessOrEqual(expr, value);
        break;
      case LT:
        model.addLessThan(expr, value);
        break;
      default:
        throw new IllegalArgumentException(String.format("State %s cannot be enforced", state));
    }
  }

  public Map<Object, Long> solveMinimize(List<Object> variables, List<Integer> coefficients) {
    IntVar[] decisionVariables = variables.stream().map(v -> {
      if (!objectVariableMap.containsKey(v)) {
        throw new IllegalArgumentException(
            String.format("Object %s is not known to the solver, try registering it with bind", v.toString()));
      }
      return objectVariableMap.get(v);
    }).toArray(IntVar[]::new);
    model.addDecisionStrategy(decisionVariables, DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
        DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE);

    model.minimize(LinearExpr.scalProd(decisionVariables, coefficients.stream().mapToInt(e -> e).toArray()));
    this.solverStatus = solver.solve(model);
    if (this.solverStatus != CpSolverStatus.OPTIMAL) {
      throw new IllegalStateException(String.format("Infeasible state with variables %s ",
          objectVariableMap.values().stream().map(IntVar::getName).collect(Collectors.toList())));
    }

    return objectVariableMap.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> solver.value(e.getValue())));
  }

  public Map<Object, Long> solveMaximise(List<Object> variables, List<Integer> coefficients) {
    IntVar[] decisionVariables = variables.stream().map(v -> {
      if (!objectVariableMap.containsKey(v)) {
        throw new IllegalArgumentException(
            String.format("Object %s is not known to the solver, try registering it with bind", v.toString()));
      }
      return objectVariableMap.get(v);
    }).toArray(IntVar[]::new);
    model.addDecisionStrategy(decisionVariables, DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
        DomainReductionStrategy.SELECT_MIN_VALUE);

    model.maximize(LinearExpr.scalProd(decisionVariables, coefficients.stream().mapToInt(e -> e).toArray()));
    this.solverStatus = solver.solve(model);
    if (this.solverStatus != CpSolverStatus.OPTIMAL) {
      throw new IllegalStateException(String.format("Infeasible state with variables %s ",
          objectVariableMap.values().stream().map(IntVar::getName).collect(Collectors.joining("|"))));
    }

    return objectVariableMap.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> solver.value(e.getValue())));
  }

  public IntVar bind(Object o, int rangeMinimum, int rangeMaximum, String description) {
    if (objectVariableMap.containsKey(o)) {
      return objectVariableMap.get(o);
    }
    IntVar modelVariable = model.newIntVar(rangeMinimum, rangeMaximum, description);
    objectVariableMap.put(o, modelVariable);
    return modelVariable;
  }

  public String describe(Object o) {
    return objectVariableMap.get(o).getName();
  }
}
