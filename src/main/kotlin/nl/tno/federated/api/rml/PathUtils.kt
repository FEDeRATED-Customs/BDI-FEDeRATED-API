// FEDeRATED node
// 
// Copyright (c) 2024-2025 Netherlands Organization for Applied Scientific Research TNO
//
// This file is part of the FEDeRATED Node API.
//
// This software is provided to Dutch Customs under the following terms:
//
// * Dutch Customs is granted a non-exclusive, non-transferable, and non-sublicensable license
//   to use, modify, and maintain the software for internal purposes only.
//
// * Dutch Customs is not permitted to disclose, distribute, or share the software, in whole or in
//   part, to any third party without prior written consent from TNO.
//
// * Any modifications made by Dutch Customs to the software must include this copyright and
//   license notice.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/**
 *
 */

package nl.tno.federated.api.rml

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import java.io.IOException
import java.io.InputStream
import java.nio.file.*
import java.nio.file.attribute.FileAttribute

/**
 * Returns the string representation of this path.
 *
 * The returned path string uses the default name [separator][FileSystem.getSeparator]
 * to separate names in the path.
 *
 * This property is a synonym to [Path.toString] function.
 */
val Path.pathString: String
    get() = toString()

/**
 * Converts this possibly relative path to an absolute path.
 *
 * If this path is already [absolute][Path.isAbsolute], returns this path unchanged.
 * Otherwise, resolves this path, usually against the default directory of the file system.
 *
 * May throw an exception if the file system is inaccessible or getting the default directory path is prohibited.
 *
 * See [Path.toAbsolutePath] for further details about the function contract and possible exceptions.
 */
fun Path.absolute(): Path = toAbsolutePath()

/**
 * Converts this possibly relative path to an absolute path and returns its string representation.
 *
 * Basically, this method is a combination of calling [absolute] and [pathString].
 *
 * May throw an exception if the file system is inaccessible or getting the default directory path is prohibited.
 *
 * See [Path.toAbsolutePath] for further details about the function contract and possible exceptions.
 */
fun Path.absolutePathString(): String = toAbsolutePath().toString()

/**
 * Deletes the file or empty directory specified by this path if it exists.
 *
 * @return `true` if the existing file was successfully deleted, `false` if the file does not exist.
 *
 * @throws DirectoryNotEmptyException if the directory exists but is not empty
 *
 * @see Files.deleteIfExists
 */
@Throws(IOException::class)
fun Path.deleteIfExists() = Files.deleteIfExists(this)

/**
 * Creates a new directory or throws an exception if there is already a file or directory located by this path.
 *
 * Note that the parent directory where this directory is going to be created must already exist.
 * If you need to create all non-existent parent directories, use [Path.createDirectories].
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 *
 * @throws FileAlreadyExistsException if there is already a file or directory located by this path
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs or the parent directory does not exist.
 * @throws UnsupportedOperationException if the [attributes ]array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createDirectory
 */
@Throws(IOException::class)
fun Path.createDirectory(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectory(this, *attributes)

/**
 * Creates a directory ensuring that all nonexistent parent directories exist by creating them first.
 *
 * If the directory already exists, this function does not throw an exception, unlike [Path.createDirectory].
 *
 * @param attributes an optional list of file attributes to set atomically when creating the directory.
 *
 * @throws FileAlreadyExistsException if there is already a file located by this path
 * (optional specific exception, some implementations may throw more general [IOException]).
 * @throws IOException if an I/O error occurs.
 * @throws UnsupportedOperationException if the [attributes ]array contains an attribute that cannot be set atomically
 *   when creating the directory.
 *
 * @see Files.createDirectories
 */
@Throws(IOException::class)
fun Path.createDirectories(vararg attributes: FileAttribute<*>): Path =
    Files.createDirectories(this, *attributes)


/**
 * Resolves the given [other] path against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.div(other: Path): Path =
    this.resolve(other)

/**
 * Resolves the given [other] path string against this path.
 *
 * This operator is a shortcut for the [Path.resolve] function.
 */
operator fun Path.div(other: String): Path =
    this.resolve(other)


/**
 * Converts the provided [path] string to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
fun Path(path: String): Path =
    Paths.get(path)

/**
 * Converts the name sequence specified with the [base] path string and a number of [subpaths] additional names
 * to a [Path] object of the [default][FileSystems.getDefault] filesystem.
 *
 * @see Paths.get
 */
fun Path(base: String, vararg subpaths: String): Path =
    Paths.get(base, *subpaths)

/**
 * Constructs a new InputStream of this file and returns it as a result.
 *
 * The [options] parameter determines how the file is opened. If no options are present then it is
 * equivalent to opening the file with the [READ][StandardOpenOption.READ] option.
 */
@Throws(IOException::class)
fun Path.inputStream(vararg options: OpenOption): InputStream {
    return Files.newInputStream(this, *options)
}