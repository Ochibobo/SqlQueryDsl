/*
    DSL 1.0.1
    Class to convert DSL to SQL Select String
    @author Warren Ochibobo
*/

class SqlSelectBuilder{
    /*
        Adding Columns
     */
    private val columns = mutableListOf<String>()
    private lateinit var table: String
    private var condition: Condition? = null

    //Define the select function 
    fun select(vararg columns: String){
        //If no columns are avaliable, throw an error
        if(columns.isEmpty()){
            throw IllegalArgumentException("At least one column should be defined")
        }
    
        if(this.columns.isNotEmpty()){
            throw IllegalStateException("Detected an attempt to redefine columns to fetch. "
                        + "Current column list: "
                        + "${this.columns}, new column list: ${columns}")
        }

        columns.forEach{
            //Append column name if it not an empty string
            k -> if(k.isNotEmpty()) this.columns.add(k)
        }
    }


    /*
        Adding Tables
    */
   
    // The from bit of the Select statement
    fun from(table: String){
        this.table = table
    }


    /*
        AND condition
    */
    //Func for actual conversion
    fun build(): String{
        //Ascertain that the table var already has a value, else throw an error
        if(!::table.isInitialized){
            throw IllegalArgumentException("Failed to build sql select - target table not specified")
        }

        //Return string representation of SqlSelectBuilder
        return toString()
    }

    //WHERE clause builder
    fun where(initializer: Condition.() -> Unit){
        condition = And().apply(initializer)
    }

    // Return String represenation of SQL Query
    override fun toString(): String{
        var columnsToFetch = if(columns.isEmpty()) "*" else columns.joinToString(", ")
        var conditionString = if(condition == null) "" else " WHERE $condition"
        return "SELECT $columnsToFetch FROM $table$conditionString;"
    } 
}
  

//Query Builder
fun query(initializer: SqlSelectBuilder.() -> Unit): SqlSelectBuilder{
    //Given that this is not a data class, fun apply is part of its instance
    return SqlSelectBuilder().apply(initializer)
}

/*
    Class to handle conditions
*/
abstract class Condition{
    //infix to call function with class object without using a dot/paranthesis across params
    infix fun String.eq(value: Any?){
        addCondition(Eq(this, value))
    }

    protected abstract fun addCondition(condition: Condition) 

    fun and(initializer: Condition.() -> Unit){
        addCondition(And().apply(initializer))
    }

    fun or(initializer: Condition.() -> Unit){
        addCondition(Or().apply(initializer))
    }
}


//Class for AND operations
class Eq(private val column: String, private val value: Any?): Condition(){
    //Ascertain value is Integer, String or Null
    init{
        if(value != null && value !is Number && value !is String){
            throw IllegalArgumentException("Only <null>, numbers and string values can be used in the 'where' clause")
        }
    }

    // Restrict nested conditions to 
    override fun addCondition(condition: Condition){
        throw IllegalArgumentException("Can't add a nested condition to the sql 'eq'")
    }

    // Return string representation of the EQ clause
    override fun toString(): String{
        return when(value){
            null -> "$column is null"
            is String -> "$column = '$value'"
            else -> "$column = $value"
        }
    }
}

// Add the AND operations
class And: CompositeCondition("and")

//Add the OR operation
class Or: CompositeCondition("or")

//Combine "And" and "Or" operations
open class CompositeCondition(val sqlOperator: String): Condition(){
    //list of conditions
    private val conditions = mutableListOf<Condition>()

    //Append conditions to list of conditions
    override fun addCondition(condition: Condition){
        conditions += condition
    }

    // Return sqlOperator string version
    override fun toString(): String{
        return if(conditions.size == 1){
            conditions.first().toString()
        } else{
            conditions.joinToString(prefix = "(", postfix = ")", separator = " $sqlOperator ")
        }
    }
}


fun main(args: Array<String>) {
    var queryString = query{
        select("*")
        from("table1")
        where{
            or {
                "column6" eq "Test"
                "column4" eq 4
            }
            "column5" eq "5"
        }
    }.build()

    println(queryString) 
}