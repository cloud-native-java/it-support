package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Data
@AllArgsConstructor
class Tree<T> {
	private Node<T> root;

	public Tree(T rootData) {
		root = new Node<T>();
		root.data = rootData;
		root.children = new ArrayList<Node<T>>();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Node<T> {
		private Node<T> parent;
		private T data;
		private List<Node<T>> children;
	}
}
