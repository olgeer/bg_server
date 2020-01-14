package com.ucsmy.itil.bg.model;

import com.alibaba.fastjson.JSON;
import com.ucsmy.itil.bg.common.Util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 2016/12/29.
 */
public class CommonExample {
    protected String orderByClause;

    protected String groupByClause;

    protected boolean distinct;

    protected int pageSize = 0;

    protected int pageIndex = 1;

    protected List<Object> values;

    protected List<Criteria> oredCriteria;

    protected List<Criteria> innoValues;

    protected String joinCols = null;

    protected boolean lockForUpdate = false;

    public CommonExample() {
        oredCriteria = new ArrayList<Criteria>();
        innoValues = new ArrayList<Criteria>();
        values = new ArrayList<Object>();
    }

    public boolean isLockForUpdate() {
        return lockForUpdate;
    }

    public void setLockForUpdate(boolean lockForUpdate) {
        this.lockForUpdate = lockForUpdate;
    }

    public String getJoinCols() {
        return joinCols;
    }

    public void setJoinCols(String joinCols) {
        this.joinCols = joinCols;
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public String getGroupByClause() {
        return groupByClause;
    }

    public void setGroupByClause(String groupByClause) {
        this.groupByClause = groupByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public List<Criteria> getInnoValues() {
        return innoValues;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    public Criteria createValueCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (innoValues.size() == 0) {
            innoValues.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        innoValues.clear();
        values.clear();
        orderByClause = null;
        groupByClause = null;
        pageIndex = 1;
        pageSize = 0;
        distinct = false;
        lockForUpdate = false;
    }

    public PreparedStatement loadValue(PreparedStatement preparedStatement) throws SQLException {
        int idx = 1;

        for (Object o : values) {
            preparedStatement.setObject(idx++, o);
        }
        return preparedStatement;
    }

    public String generateCriteriaSql() {
        String retString = "";
        //values.clear();

        if (oredCriteria.isEmpty() == false) {
            for (int i = 0; i < oredCriteria.size(); i++) {
                if (i > 0) retString += " or";
                retString += buildAndCriteriaSql(oredCriteria.get(i));
            }
        }
        if (retString.length() > 0) retString = " where" + retString;

        if (groupByClause != null) {
            retString += " group by " + Util.mysqlEscape(groupByClause);
        }

        if (orderByClause != null) {
            retString += " order by " + Util.mysqlEscape(orderByClause);
        }

        if (pageSize > 0 && pageIndex > 0) {
            retString += " limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
        }

        if (lockForUpdate) retString += " for update";

        return retString;
    }

    public String generateCriteriaSqlWithValue() {
        String retString = "";
        //values.clear();

        if (!oredCriteria.isEmpty()) {
            for (int i = 0; i < oredCriteria.size(); i++) {
                if (i > 0) retString += " or";
                retString += buildAndCriteriaSqlWithValue(oredCriteria.get(i));
            }
        }
        if (retString.length() > 0) retString = " where" + retString;

        if (groupByClause != null) {
            retString += " group by " + Util.mysqlEscape(groupByClause);
        }

        if (orderByClause != null) {
            retString += " order by " + Util.mysqlEscape(orderByClause);
        }

        if (pageSize > 0 && pageIndex > 0) {
            retString += " limit " + ((pageIndex - 1) * pageSize) + "," + pageSize;
        }

        if (lockForUpdate) retString += " for update";

        return retString;
    }

    public String generateUpdateSql() {
        String retString = "";
        if (!innoValues.isEmpty()) {
            Criteria criteria = innoValues.get(0);
            if (criteria.isValid()) {
                List<Criterion> criterions = criteria.getAllCriteria();
                for (Criterion criterion : criterions) {
                    if (criterion.isSingleValue()) {
                        retString += criterion.getCondition() + "?,";
                        values.add(criterion.getValue());
                    }
                    if (criterion.isNoValue()) {
                        retString += criterion.getCondition() + ",";
                    }
                }
                if (retString.length() > 0) retString = retString.substring(0, retString.length() - 1);
            }
        }

        return retString;
    }

    public String generateInsertSql() {
        String cols = "";
        String vals = "";

        if (!innoValues.isEmpty()) {
            Criteria criteria = innoValues.get(0);
            if (criteria.isValid()) {
                List<Criterion> criterions = criteria.getAllCriteria();
                for (Criterion criterion : criterions) {
                    if (criterion.isSingleValue()) {
                        cols += criterion.getCondition() + ",";
                        vals += "?,";
                        values.add(criterion.getValue());
                    }
                }
                if (cols.length() > 0) {
                    cols = cols.substring(0, cols.length() - 1);
                    vals = vals.substring(0, vals.length() - 1);
                    cols = " (" + cols + ")";
                    vals = " values(" + vals + ")";
                }
            }
        }

        return cols + vals;
    }

    private String buildAndCriteriaSql(Criteria criteria) {
        String retString = "";

        if (criteria.isValid()) {
            List<Criterion> criterions = criteria.getAllCriteria();
            for (Criterion criterion : criterions) {
                if (criterion.isNoValue()) {
                    retString += criterion.getCondition();

                }
                if (criterion.isSingleValue()) {
                    //String quot = Util.getQuot(criterion.getValue());
                    //retString += criterion.getCondition() + quot + criterion.getValue() + quot;
                    if (criterion.getCondition().endsWith("like")) {
                        retString += criterion.getCondition() + " '%" + Util.mysqlEscape((String) criterion.getValue()) + "%' ";
                    } else if (criterion.getCondition().endsWith("regexp")) {
                        retString += criterion.getCondition() + " '" + Util.mysqlEscape((String) criterion.getValue()) + "'";
                    } else {
                        retString += criterion.getCondition() + " ? ";
                        values.add(criterion.getValue());
                    }
                }
                if (criterion.isBetweenValue()) {
                    //String quot = Util.getQuot(criterion.getValue());
                    //retString += criterion.getCondition() + " " + quot + criterion.getValue() + quot + " and " + quot + criterion.getSecondValue() + quot;
                    retString += criterion.getCondition() + " ? and ?";
                    values.add(criterion.getValue());
                    values.add(criterion.getSecondValue());
                }
                if (criterion.isListValue()) {
                    List<Object> objectList = (List<Object>) criterion.getValue();
                    //String quot = Util.getQuot(objectList.get(0));
                    retString += criterion.getCondition() + " (";
                    for (Object object : objectList) {
                        //retString += quot + object + quot + ",";
                        retString += "?,";
                        values.add(object);
                    }
                    retString = retString.substring(0, retString.length() - 1) + ")";
                }
                retString += " and ";
            }
            if (!criterions.isEmpty()) retString = retString.substring(0, retString.length() - 5);
        }
        if (retString.length() > 0) retString = " " + retString;
        return retString;
    }

    private String buildAndCriteriaSqlWithValue(Criteria criteria) {
        String retString = "";

        if (criteria.isValid()) {
            List<Criterion> criterions = criteria.getAllCriteria();
            for (Criterion criterion : criterions) {
                if (criterion.isNoValue()) {
                    retString += criterion.getCondition();

                }
                if (criterion.isSingleValue()) {
                    String quot = Util.getQuot(criterion.getValue());
                    if (criterion.getCondition().endsWith("like")) {
                        retString += criterion.getCondition() + " '%" + Util.mysqlEscape((String) criterion.getValue()) + "%' ";
                    } else if (criterion.getCondition().endsWith("regexp")) {
                        retString += criterion.getCondition() + " '" + Util.mysqlEscape((String) criterion.getValue()) + "'";
                    } else {
                        retString += criterion.getCondition() + quot + criterion.getValue() + quot;
                        //values.add(criterion.getValue());
                    }
                }
                if (criterion.isBetweenValue()) {
                    String quot = Util.getQuot(criterion.getValue());
                    retString += criterion.getCondition() + " " + quot + criterion.getValue() + quot + " and " + quot + criterion.getSecondValue() + quot;
                    //retString += criterion.getCondition() + " ? and ?";
                    //values.add(criterion.getValue());
                    //values.add(criterion.getSecondValue());
                }
                if (criterion.isListValue()) {
                    List<Object> objectList = (List<Object>) criterion.getValue();
                    String quot = Util.getQuot(objectList.get(0));
                    retString += criterion.getCondition() + " (";
                    for (Object object : objectList) {
                        retString += quot + object + quot + ",";
                        //retString += "?,";
                        //values.add(object);
                    }
                    retString = retString.substring(0, retString.length() - 1) + ")";
                }
                retString += " and ";
            }
            if (!criterions.isEmpty()) retString = retString.substring(0, retString.length() - 5);
        }
        if (retString.length() > 0) retString = " " + retString;
        return retString;
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria addKeyValue(String col, Object val) {
            /*if(val.getClass().getName().contains("String")) {
                val=((String) val).replaceAll("\\\\","\\\\\\\\");
            }*/
            addCriterion(Util.keywordFilter(col), val, col);

            return (Criteria) this;
        }

        public Criteria setKeyValue(String col, Object val) {
            addCriterion(Util.keywordFilter(col) + "=", val, col);
            return (Criteria) this;
        }

        public Criteria setKeyValue2Null(String col) {
            addCriterion(Util.keywordFilter(col) + "=NULL");
            return (Criteria) this;
        }

        public Criteria setKeyValueFromKey(String col, String col2) {
            addCriterion(Util.keywordFilter(col) + "=" + Util.keywordFilter(col2));
            return (Criteria) this;
        }

        public Criteria andColumnEqualColumn(String col, String col2) {
            addCriterion(Util.keywordFilter(col) + " = " + Util.keywordFilter(col2));
            return (Criteria) this;
        }

        public Criteria andCriterion(String criterion) {
            addCriterion(criterion);
            return (Criteria) this;
        }

        public Criteria andColumnIsNull(String col) {
            addCriterion(Util.keywordFilter(col) + " is null");
            return (Criteria) this;
        }

        public Criteria andColumnIsNotNull(String col) {
            addCriterion(Util.keywordFilter(col) + " is not null");
            return (Criteria) this;
        }

        public Criteria andColumnEqualTo(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " =", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnNotEqualTo(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " <>", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnGreaterThan(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " >", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnGreaterThanOrEqualTo(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " >=", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnLessThan(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " <", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnLessThanOrEqualTo(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " <=", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnRegexp(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " regexp", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnLike(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " like", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnNotLike(String col, Object value) {
            addCriterion(Util.keywordFilter(col) + " not like", value, col);
            return (Criteria) this;
        }

        public Criteria andColumnIn(String col, List<Object> values) {
            addCriterion(Util.keywordFilter(col) + " in", values, col);
            return (Criteria) this;
        }

        public Criteria andColumnNotIn(String col, List<Object> values) {
            addCriterion(Util.keywordFilter(col) + " not in", values, col);
            return (Criteria) this;
        }

        public Criteria andColumnBetween(String col, Object value1, Object value2) {
            addCriterion(Util.keywordFilter(col) + " between", value1, value2, col);
            return (Criteria) this;
        }

        public Criteria andColumnNotBetween(String col, Object value1, Object value2) {
            addCriterion(Util.keywordFilter(col) + " not between", value1, value2, col);
            return (Criteria) this;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
