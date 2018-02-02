package madgik.exareme.master.queryProcessor.sparql;

import java.util.ArrayList;
import java.util.List;

public class UnionWrapperInfo {

	private int alias;
	private int partitions;
	int size;

	private List<Integer> info;

	public UnionWrapperInfo(int alias, int partitions, int size) {
		super();
		this.alias = alias;
		this.partitions = partitions;
		this.size = size;
		this.info = new ArrayList<Integer>();
	}

	public void addTableInfo(int propNo, int inverse) {
		this.info.add(propNo);
		this.info.add(inverse);
	}

	public void addFilter(int filter) {
		this.info.add(filter);
	}

	public void inverseTables() {
		for (int i = 1; i < info.size(); i += 2) {
			if (info.get(i) > 1) {
				// "single" column uwi
				return;
			}
		}

		for (int i = 1; i < info.size(); i += 2) {

			if (info.get(i) == 0) {
				info.set(i, 1);
			} else {
				info.set(i, 0);
			}
		}

	}

	public String getSQL() {
		String result = "create  virtual table temp.memorywrapperprop" + alias + " using unionwrapper(" + partitions
				+ ", " + size;
		for (int i = 0; i < info.size(); i += 2) {
			result += ", " + info.get(i) + ", " + info.get(i + 1);

			if (info.get(i + 1) > 1) {
				result += ", " + info.get(i + 2);
				i++;
			}
		}
		result += ")";
		return result;
	}

	public int getAlias() {
		return alias;
	}

}
