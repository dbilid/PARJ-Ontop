package it.unibz.inf.ontop.sql;
import java.util.*;

public class Timer {

    private List<Test> list;
    private Map<String, Test> map;

    public Timer() {
		list = new LinkedList<Test>();
		map = new HashMap<String, Test>();
    }

    public void newTest(String name) {
		Test test = new Test(name);
		list.add(test);
		map.put(name, test);
    }

    public void endTest(String name) {
		map.get(name).end();
    }

    public void setError(String name, String message) {
		map.get(name).setError(message);
    }

    public double getDuration(String name) {
		return map.get(name).getDuration();
    }

    public String printResults() {
		StringBuffer str = new StringBuffer(500);
		for (Test test : list) {
			str.append(test.getName());
			str.append(": \t");
			if (test.isError()) {
				str.append("ERROR: ");
				str.append(test.getError());
			}
			else {
				str.append(test.getDuration());
				str.append(" ms");
			}
			str.append("\n");
		}
		return str.toString();
    }

    ////////////////////////////////////////////7
    // Inner class

    private class Test {
		private String name;
		private String error = null;
		private long start, end;
		private double duration;

		public Test(String name) {
			this.name = name;
			start = System.nanoTime();
		}
		public void end() {
			end = System.nanoTime();
			duration = (end - start) / 1000000.0;
		}
		public String getName() {
			return name;
		}
		public double getDuration() {
			return duration;
		}
		public void setError(String message) {
			error = message;
		}
		public boolean isError() {
			return error != null;
		}
		public String getError() {
			return error;
		}
    }
}