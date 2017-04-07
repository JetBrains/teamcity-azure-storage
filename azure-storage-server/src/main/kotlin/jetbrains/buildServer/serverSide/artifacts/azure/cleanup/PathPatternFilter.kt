/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See LICENSE in the project root for license information.
 */

package jetbrains.buildServer.serverSide.artifacts.azure.cleanup

import jetbrains.buildServer.util.CollectionsUtil
import jetbrains.buildServer.util.IncludeExcludeRules
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.pathMatcher.AntPatternTreeMatcher
import java.util.*

/**
 * @author vbedrosova
 */
class PathPatternFilter(patterns: String) {

    private val myIncludePatterns = ArrayList<String>()
    private val myExcludePatterns = ArrayList<String>()

    init {
        var patterns = patterns
        if (StringUtil.isEmptyOrSpaces(patterns)) {
            patterns = "+:" + DEFAULT_INCLUDE_RULE
        }

        val rules = IncludeExcludeRules(patterns)
        for (r in rules.rules) {
            if (r.isInclude) {
                myIncludePatterns.add(r.rule)
            } else {
                myExcludePatterns.add(r.rule)
            }
        }
    }

    fun filterPaths(paths: List<String>): List<String> {
        if (isIncludeAll) return paths

        val files = AntPatternTreeMatcher.scan(createPathTree(paths), myIncludePatterns, myExcludePatterns, AntPatternTreeMatcher.ScanOption.LEAFS_ONLY)
        return CollectionsUtil.convertCollection<String, PathNode>(files, { it.path })
    }

    private val isIncludeAll: Boolean
        get() = myExcludePatterns.isEmpty() && myIncludePatterns.size == 1 && INCLUDE_ALL_PATTERNS.contains(myIncludePatterns[0])

    internal fun createPathTree(paths: List<String>):
            /*package local*/ PathNode {
        val pathsList = ArrayList(paths)
        Collections.sort(pathsList)

        val root = PathNode("")

        for (path in pathsList) {
            var parent = root
            for (part in path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val parentPath = parent.path
                parent = parent.child(if (parentPath.isEmpty()) part else parentPath + "/" + part)
            }
        }

        return root
    }

    internal class PathNode(path: String) : jetbrains.buildServer.util.pathMatcher.PathNode<PathNode> {
        val path: String
        private var myChildren: MutableSet<PathNode>? = null

        init {
            var path = path
            if (path.endsWith("/")) path = path.substring(0, path.length - 1)
            if (path.startsWith("/")) path = path.substring(1)
            this.path = path
            myChildren = null
        }

        override fun getName(): String {
            val slashIndex = path.lastIndexOf("/")
            return if (slashIndex < 0) path else path.substring(slashIndex + 1)
        }

        override fun getChildren(): Iterable<PathNode>? {
            return myChildren
        }

        fun child(path: String): PathNode {
            if (myChildren == null) myChildren = HashSet<PathNode>()

            for (c in myChildren!!) {
                if (path == c.path) return c
            }

            val child = PathNode(path)
            myChildren!!.add(child)
            return child
        }
    }

    internal val includePatterns: List<String>
        get() = myIncludePatterns

    internal val excludePatterns: List<String>
        get() = myExcludePatterns

    companion object {
        val DEFAULT_INCLUDE_RULE = "**/*"
        val INCLUDE_ALL_PATTERNS: Collection<String> = Arrays.asList(DEFAULT_INCLUDE_RULE, "+:**/*", "", "+.")
    }
}
