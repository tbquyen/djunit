# djUnit
djUnit plugin is JUnit TestRunner with the custom classloader.
djUnit ClassLoader modifies class in loading to JVM, and runs tests using the modified class. The following functions can now be used easily.

- Testing using Virtual Mock Objects

djUnit can perform tests of JUnit, and useage is easy(same as JUnit).

> The current version has removed the Eclipse plugin and supports the EclEmma library for code coverage.

## Supported Java Versions
djUnit currently supports the following ASM 9.x versions:

| Java version | ASM version |
| :----------- | -----------: |
| 23 | 9.7 |
| 22 | 9.6 |
| 21 | 9.5 |
| 20 | 9.4 |
| 19 | 9.3 |
| 18 | 9.2 |
| 17 | 9.1 |
| 16 | 9.0 |

https://asm.ow2.io/versions.html

## Installation

Download the djUnit library and add it to your project’s class path.

### Dependencies

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.ow2.asm</groupId>
    <artifactId>asm</artifactId>
    <version>9.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy-agent</artifactId>
    <version>1.15.1</version>
    <scope>test</scope>
</dependency>
```

## Testing using Virtual Mock Objects
djUnit uses ASM and BCEL, and has an added original extension.<br>
If Virtual Mock Objects is used, it enables creation of Mock Object at low cost, and it can also write simple tests.

You can find more detailed information on this Wiki.

## Tips
Default settings can be overridden by adding VM Arguments:

| VM Arguments                              | Value                                       | Default               | Required |
| :---------------------------------------- | :------------------------------------------ | :-------------------- | :------: |
| javaagent                                 | /jarpath/djunit.jar                         | Dynamic loading       |   No     |
| jp.co.dgic.virtualmock.usevirtualmock     | use virtual mock                            | `true`                |   No     |
| jp.co.dgic.virtualmock.include.class      | include class                               | `null`                |   No     |
| jp.co.dgic.virtualmock.ignore.library     | ignore library                              | `false`               |   No     |
| jp.co.dgic.virtualmock.notignore.patterns | not ignore patterns                         | `false`               |   No     |
| jp.co.dgic.virtualmock.coverage.methods   | methods coverage                            | `$jacocoInit`         |   No     |
| jp.co.dgic.project.source.dir             | path to your class folder                   | from `java.class.path`|   No     |
| jp.co.dgic.junit.excluded.paths           | excluded paths                              | `null`                |   No     |

## Release Note

> Version 1.0.1 [2024/09/21]
- fix: resolve issue with retrieving project source directory in classpath
> Version 0.8.6 for Eclipse 3.2, 3.3, 3.4, 3.5 Released.[2011/10/17]

## License

```xml
###############################################################################
# Copyright (C)2004 dGIC Corporation.
#
# This file is part of djUnit plugin.
#
# djUnit plugin is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published
# by the Free Software Foundation; either version 2 of the License,
# or (at your option) any later version.
#
# djUnit plugin is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with djUnit plugin; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
# USA
#
###############################################################################
