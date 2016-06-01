package org.activeorm;

import org.activeorm.database.Database;
import org.activeorm.mapping.AttributeMapping;
import org.activeorm.mapping.ObjectMapping;
import org.activeorm.mapping.relationships.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Francis on 9/04/16.
 * Project Active-ORM.
 */
public class ActiveRecord {

	/**
	 * The {@link ObjectMapping} used to map the object.
	 * Constructed in the constructor of {@link ActiveRecord}.
	 */
	private final ObjectMapping mapping;

	/**
	 * Constructs a new {@link ActiveRecord}.
	 */
	public ActiveRecord() {
		this.mapping = new ObjectMapping(getClass(), this);
	}

	/**
	 * Gets the primary key value of this {@link ActiveRecord}.
	 *
	 * @return the primary key of this {@link ActiveRecord}.
	 */
	public int getId() {
		return (int) mapping.primaryKey.field.getValue();
	}

	/**
	 * Either executes a insert or update sql statement
	 * depending on if the object has been persisted yet.
	 *
	 * @return true if successfully executed the statement else false.
	 */
	public final boolean save() {
		// Find all the modified fields
		final List<AttributeMapping> modifications = new ArrayList<>();
		for (final AttributeMapping attribute : this.mapping.attributes) {
			if (attribute.field.hasBeenModified()) {
				modifications.add(attribute);
			}
		}

		// Create a array of column names based off the modifed fields
		final String[] columns = new String[modifications.size()];
		for (int i = 0; i < modifications.size(); i++) {
			columns[i] = modifications.get(i).column.name();
		}

		// The values of the modified fields
		final Object[] values = new Object[modifications.size()];
		for (int i = 0; i < modifications.size(); i++) {
			values[i] = modifications.get(i).field.getValue();
		}

		// Get the database instance
		final Database database = Database.getInstance();

		// Generate the sql for the statement
		final String sql;
		if (mapping.persisted) {
			sql = database.sql.update(mapping.table.name(), columns, new String[]{mapping.primaryKey.column.name()}, new String[]{"="});
		} else {
			sql = database.sql.insert(mapping.table.name(), columns);
		}

		// Execute the sql and return the amount of records modified, normally one.
		final int result = database.execute(sql, values, mapping.primaryKey);
		if (result > 0) {
			mapping.persisted = true;
		}

		// Save all the relationships
		for (final Relationship relationship : mapping.relationships) {
			relationship.save();
		}

		// If the amount of records changed is larger then 0 return true
		return result > 0;
	}

	/**
	 * Deletes the record from the database.
	 *
	 * @return true if successful, else false.
	 */
	public final boolean destroy() {
		// Create an array which contains the primary key column name
		final String[] columns = new String[]{mapping.primaryKey.column.name()};

		// Create an array which contains the operators for the column to value comparison
		final String[] operators = new String[]{"="};

		// Create an array which contains the primary key value
		final Object[] values = new Object[]{mapping.primaryKey.field.getValue()};

		// Get the database instance
		final Database database = Database.getInstance();

		// Generate the sql
		final String sql = database.sql.delete(mapping.table.name(), columns, operators);

		// Execute the sql and return the amount of records modified, normally one.
		final int result = database.execute(sql, values);
		if (result > 0) {
			mapping.persisted = false;
		}

		// If the amount of records changed is larger then 0 return true
		return result > 0;
	}

	/**
	 * Deletes the record from the database.
	 *
	 * @return true if successful, else false.
	 */
	public final boolean destroyAll() {
		// Destroy all relationship data
		for (final Relationship relationship : mapping.relationships) {
			relationship.destroyAll();
		}

		// Destroy this record
		return destroy();
	}
}