package cz.fg.oss.pmptt.util;

import cz.fg.oss.pmptt.model.Hierarchy;
import cz.fg.oss.pmptt.model.HierarchyItem;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class StructureLoader {
	private static final Pattern WHITE_SPACE = Pattern.compile("(\\s+)(.+)");

	public static void loadHierarchy(Resource resource, Hierarchy hierarchy) {
		try (final InputStream is = resource.getInputStream()) {
			final List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
			final Stack<HierarchyItem> stack = new Stack<>();
			HierarchyItem lastHierarchyItem = null;
			int lastIndentation = 0;
			for (String line : lines) {
				int indentation = getWhiteSpaceIndentation(line);
				if (indentation > lastIndentation) {
					stack.push(lastHierarchyItem);
				} else if (indentation < lastIndentation) {
					stack.pop();
				}

				final String code = line.trim();
				final HierarchyItem hierarchyItem;
				if (indentation == 0) {
					hierarchyItem = hierarchy.createRootItem(code);
				} else {
					hierarchyItem = hierarchy.createItem(code, stack.peek().getCode());
				}

				lastIndentation = indentation;
				lastHierarchyItem = hierarchyItem;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String storeHierarchy(Hierarchy hierarchy) {
		final StringBuilder sb = new StringBuilder();
		for (HierarchyItem rootItem : hierarchy.getRootItems()) {
			sb.append(rootItem.getCode()).append("\n");
			printChildren(hierarchy, rootItem, sb);
		}
		return sb.toString();
	}

	public static String storeHierarchyAndPrintWithBounds(Hierarchy hierarchy) {
		final StringBuilder sb = new StringBuilder();
		for (HierarchyItem rootItem : hierarchy.getRootItems()) {
			sb.append(rootItem.getCode()).append(" (").append(rootItem.getLeftBound()).append("-").append(rootItem.getRightBound()).append(")").append("\n");
			printChildrenWithBounds(hierarchy, rootItem, sb);
		}
		return sb.toString();
	}

	private static void printChildren(Hierarchy hierarchy, HierarchyItem item, StringBuilder sb) {
		for (HierarchyItem childItem : hierarchy.getChildItems(item.getCode())) {
			for(int i = 0; i < childItem.getLevel() - 1; i++) {
				sb.append("    ");
			}
			sb.append(childItem.getCode()).append("\n");
			printChildren(hierarchy, childItem, sb);
		}
	}

	private static void printChildrenWithBounds(Hierarchy hierarchy, HierarchyItem item, StringBuilder sb) {
		for (HierarchyItem childItem : hierarchy.getChildItems(item.getCode())) {
			for(int i = 0; i < childItem.getLevel() - 1; i++) {
				sb.append("    ");
			}
			sb.append(childItem.getCode()).append(" (").append(childItem.getLeftBound()).append("-").append(childItem.getRightBound()).append(")").append("\n");
			printChildrenWithBounds(hierarchy, childItem, sb);
		}
	}

	private static int getWhiteSpaceIndentation(String line) {
		final Matcher matcher = WHITE_SPACE.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1).length();
		} else {
			return 0;
		}
	}
}
