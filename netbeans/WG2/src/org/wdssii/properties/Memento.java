package org.wdssii.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of properties that belong together.
 * Each property is referenced by a Key object 'K'
 * 
 * @author Robert Toomey
 */
public abstract class Memento<K> {

	private final static Logger LOG = LoggerFactory.getLogger(Memento.class);

        /** A memento propery */
	public static class Property {

		public boolean use;
		public Object value;

		/**
		 * Created by initProperty
		 */
		public Property(Object o) {
			value = o;
		}

		/**
		 * Created by set property
		 */
		public Property(Object o, boolean u) {
			this(o);
			use = u;
		}

		/**
		 * Get the value stored inside us
		 */
                @SuppressWarnings("unchecked")
		public <T extends Object> T getValue() {
			return (T) value;
		}
	}

	public Memento() {
	}

	/**
	 * Create a full copy of another mememto
	 */
	public Memento(Memento m) {
	}
 
	/**
	 * Sync to another memento by only copying what is wanted to be changed.
	 *
	 * @param m
	 */
	public abstract void syncToMemento(Memento m);

	public abstract void initProperty(K key, Object stuff);

	public abstract void setProperty(K key, Object stuff);

	public abstract Property getProperty(K key);

	public abstract <T extends Object> T getPropertyValue(K key);
}