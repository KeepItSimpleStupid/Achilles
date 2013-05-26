package info.archinnov.achilles.proxy.wrapper;

import static info.archinnov.achilles.helper.ThriftLoggerHelper.format;
import static me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality.EQUAL;
import info.archinnov.achilles.composite.ThriftCompositeFactory;
import info.archinnov.achilles.dao.ThriftGenericWideRowDao;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.exception.AchillesException;
import info.archinnov.achilles.helper.ThriftPropertyHelper;
import info.archinnov.achilles.iterator.ThriftCounterSliceIterator;
import info.archinnov.achilles.iterator.factory.ThriftIteratorFactory;
import info.archinnov.achilles.iterator.factory.ThriftKeyValueFactory;
import info.archinnov.achilles.proxy.wrapper.builder.ThriftCounterWrapperBuilder;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.Counter;
import info.archinnov.achilles.type.KeyValue;
import info.archinnov.achilles.type.KeyValueIterator;

import java.util.List;

import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HCounterColumn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThriftCounterWideMapWrapper
 * 
 * @author DuyHai DOAN
 * 
 */
public class ThriftCounterWideMapWrapper<K> extends ThriftAbstractWideMapWrapper<K, Counter>
{

	private static Logger log = LoggerFactory.getLogger(ThriftCounterWideMapWrapper.class);

	private Object id;
	private ThriftGenericWideRowDao wideMapCounterDao;
	private PropertyMeta<K, Counter> propertyMeta;

	private ThriftPropertyHelper thriftPropertyHelper;
	private ThriftKeyValueFactory thriftKeyValueFactory;
	private ThriftIteratorFactory thriftIteratorFactory;
	private ThriftCompositeFactory thriftCompositeFactory;

	@Override
	public Counter get(K key)
	{
		log.trace("Get counter value having key {}", key);
		Composite comp = thriftCompositeFactory.createForQuery(propertyMeta, key, EQUAL);

		return ThriftCounterWrapperBuilder.builder(context) //
				.columnName(comp)
				.counterDao(wideMapCounterDao)
				.key(id)
				.readLevel(propertyMeta.getReadConsistencyLevel())
				.writeLevel(propertyMeta.getWriteConsistencyLevel())
				.build();

	}

	@Override
	public void insert(K key, Counter value, int ttl)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot insert counter value with ttl");
	}

	@Override
	public void insert(K key, Counter value)
	{
		log.trace("Insert counter value {} with key {}", value, key);
		Composite comp = thriftCompositeFactory.createBaseComposite(propertyMeta, key);
		try
		{
			wideMapCounterDao.incrementCounter(id, comp, value.get());
		}
		catch (Exception e)
		{
			log.trace("Exception raised, clean up consistency levels");
			context.cleanUpFlushContext();
			throw new AchillesException(e);
		}
	}

	@Override
	public List<KeyValue<K, Counter>> find(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering)
	{
		thriftPropertyHelper.checkBounds(propertyMeta, start, end, ordering, false);

		Composite[] queryComps = thriftCompositeFactory.createForQuery(propertyMeta, start, end,
				bounds, ordering);
		if (log.isTraceEnabled())
		{
			log.trace("Find key/value pairs in range {} / {} with bounding {} and ordering {}",
					format(queryComps[0]), format(queryComps[1]), bounds.name(), ordering.name());
		}

		List<HCounterColumn<Composite>> hColumns = wideMapCounterDao.findCounterColumnsRange(id,
				queryComps[0], queryComps[1], count, ordering.isReverse());

		return thriftKeyValueFactory.createCounterKeyValueList(context, propertyMeta, hColumns);
	}

	@Override
	public List<Counter> findValues(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering)
	{
		thriftPropertyHelper.checkBounds(propertyMeta, start, end, ordering, false);

		Composite[] queryComps = thriftCompositeFactory.createForQuery( //
				propertyMeta, start, end, bounds, ordering);

		if (log.isTraceEnabled())
		{
			log.trace("Find value in range {} / {} with bounding {} and ordering {}",
					format(queryComps[0]), format(queryComps[1]), bounds.name(), ordering.name());
		}
		List<HCounterColumn<Composite>> hColumns = wideMapCounterDao.findCounterColumnsRange(id,
				queryComps[0], queryComps[1], count, ordering.isReverse());

		return thriftKeyValueFactory.createCounterValueList(context, propertyMeta, hColumns);
	}

	@Override
	public List<K> findKeys(K start, K end, int count, BoundingMode bounds, OrderingMode ordering)
	{
		thriftPropertyHelper.checkBounds(propertyMeta, start, end, ordering, false);
		Composite[] queryComps = thriftCompositeFactory.createForQuery( //
				propertyMeta, start, end, bounds, ordering);

		if (log.isTraceEnabled())
		{
			log.trace("Find keys in range {} / {} with bounding {} and ordering {}",
					format(queryComps[0]), format(queryComps[1]), bounds.name(), ordering.name());
		}

		List<HCounterColumn<Composite>> hColumns = wideMapCounterDao.findCounterColumnsRange(id,
				queryComps[0], queryComps[1], count, ordering.isReverse());
		return thriftKeyValueFactory.createCounterKeyList(propertyMeta, hColumns);
	}

	@Override
	public KeyValueIterator<K, Counter> iterator(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering)
	{
		Composite[] queryComps = thriftCompositeFactory.createForQuery( //
				propertyMeta, start, end, bounds, ordering);

		if (log.isTraceEnabled())
		{
			log
					.trace("Iterate in range {} / {} with bounding {} and ordering {} and batch of {} elements",
							format(queryComps[0]), format(queryComps[1]), bounds.name(),
							ordering.name(), count);
		}
		ThriftCounterSliceIterator<?> columnSliceIterator = wideMapCounterDao
				.getCounterColumnsIterator(id, queryComps[0], queryComps[1], ordering.isReverse(),
						count);

		return thriftIteratorFactory.createCounterKeyValueIterator(context, columnSliceIterator,
				propertyMeta);
	}

	@Override
	public void remove(K key)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void remove(K start, K end, BoundingMode bounds)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeFirst(int count)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeLast(int count)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public Counter get(K key, ConsistencyLevel readLevel)
	{
		return get(key);
	}

	@Override
	public void insert(K key, Counter value, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public void insert(K key, Counter value, int ttl, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot insert counter value with ttl");
	}

	@Override
	public List<KeyValue<K, Counter>> find(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> find(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> findBoundsExclusive(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> findReverse(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> findReverseBoundsExclusive(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValue<K, Counter> findFirst(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> findFirst(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValue<K, Counter> findLast(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<KeyValue<K, Counter>> findLast(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findValues(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findValues(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findBoundsExclusiveValues(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findReverseValues(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findReverseBoundsExclusiveValues(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public Counter findFirstValue(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findFirstValues(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public Counter findLastValue(ConsistencyLevel readLevel)
	{
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<Counter> findLastValues(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findKeys(K start, K end, int count, BoundingMode bounds, OrderingMode ordering,
			ConsistencyLevel readLevel)
	{
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findKeys(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findBoundsExclusiveKeys(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findReverseKeys(K start, K end, int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findReverseBoundsExclusiveKeys(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public K findFirstKey(ConsistencyLevel readLevel)
	{
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findFirstKeys(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public K findLastKey(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public List<K> findLastKeys(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iterator(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iterator(K start, K end, int count, BoundingMode bounds,
			OrderingMode ordering, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iterator(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iterator(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iteratorBoundsExclusive(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iteratorReverse(ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iteratorReverse(int count, ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iteratorReverse(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public KeyValueIterator<K, Counter> iteratorReverseBoundsExclusive(K start, K end, int count,
			ConsistencyLevel readLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException(
				"Please set runtime consistency level at Counter level instead of at WideMap level");
	}

	@Override
	public void remove(K key, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void remove(K start, K end, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void remove(K start, K end, BoundingMode bounds, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeBoundsExclusive(K start, K end, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeFirst(ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeFirst(int count, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeLast(ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	@Override
	public void removeLast(int count, ConsistencyLevel writeLevel)
	{
		context.cleanUpFlushContext();
		throw new UnsupportedOperationException("Cannot remove counter value");
	}

	public void setId(Object id)
	{
		this.id = id;
	}

	public void setPropertyMeta(PropertyMeta<K, Counter> propertyMeta)
	{
		this.propertyMeta = propertyMeta;
	}

	public void setCompositeHelper(ThriftPropertyHelper thriftPropertyHelper)
	{
		this.thriftPropertyHelper = thriftPropertyHelper;
	}

	public void setKeyValueFactory(ThriftKeyValueFactory thriftKeyValueFactory)
	{
		this.thriftKeyValueFactory = thriftKeyValueFactory;
	}

	public void setIteratorFactory(ThriftIteratorFactory thriftIteratorFactory)
	{
		this.thriftIteratorFactory = thriftIteratorFactory;
	}

	public void setCompositeKeyFactory(ThriftCompositeFactory thriftCompositeFactory)
	{
		this.thriftCompositeFactory = thriftCompositeFactory;
	}

	public void setWideMapCounterDao(ThriftGenericWideRowDao wideMapCounterDao)
	{
		this.wideMapCounterDao = wideMapCounterDao;
	}
}
