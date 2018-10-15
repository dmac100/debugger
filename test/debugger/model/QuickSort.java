package debugger.model;

import java.util.ArrayList;
import java.util.List;

public class QuickSort {
	public static List<Integer> sort(List<Integer> values) {
		if(values.size() <= 1) {
			return values;
		}
		
		int pivot = values.get(0);
		List<Integer> left = new ArrayList<>();
		List<Integer> right = new ArrayList<>();
		for(int x = 1; x < values.size(); x++) {
			if(values.get(x) < pivot) {
				left.add(values.get(x));
			} else {
				right.add(values.get(x));
			}
		}
		
		List<Integer> result = new ArrayList<>();
		result.addAll(sort(left));
		result.add(pivot);
		result.addAll(sort(right));
		
		return result;
	}
}
