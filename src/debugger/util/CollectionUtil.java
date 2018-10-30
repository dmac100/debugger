package debugger.util;

import java.util.List;
import java.util.function.Predicate;

public class CollectionUtil {
	public static <T> T getLast(List<T> list) {
		return list.get(list.size() - 1);
	}
	
	public static <T> int lastIndexOf(List<T> list, Predicate<? super T> filter) {
		int index = -1;
		for(int i = 0; i < list.size(); i++) {
			if(filter.test(list.get(i))) {
				index = i;
			}
		}
		return index;
	}
}