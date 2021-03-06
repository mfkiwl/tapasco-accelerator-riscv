/*
 * Copyright (c) 2014-2020 Embedded Systems and Applications, TU Darmstadt.
 *
 * This file is part of TaPaSCo 
 * (see https://github.com/esa-tu-darmstadt/tapasco).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef TLKM_SLOT_H__
#define TLKM_SLOT_H__

#define PLATFORM_NUM_SLOTS 128

#define TLKM_SLOTS                                                             \
	_SLOT(0)                                                               \
	_SLOT(1)                                                               \
	_SLOT(2)                                                               \
	_SLOT(3)                                                               \
	_SLOT(4)                                                               \
	_SLOT(5)                                                               \
	_SLOT(6)                                                               \
	_SLOT(7)                                                               \
	_SLOT(8)                                                               \
	_SLOT(9)                                                               \
	_SLOT(10)                                                              \
	_SLOT(11)                                                              \
	_SLOT(12)                                                              \
	_SLOT(13)                                                              \
	_SLOT(14)                                                              \
	_SLOT(15)                                                              \
	_SLOT(16)                                                              \
	_SLOT(17)                                                              \
	_SLOT(18)                                                              \
	_SLOT(19)                                                              \
	_SLOT(20)                                                              \
	_SLOT(21)                                                              \
	_SLOT(22)                                                              \
	_SLOT(23)                                                              \
	_SLOT(24)                                                              \
	_SLOT(25)                                                              \
	_SLOT(26)                                                              \
	_SLOT(27)                                                              \
	_SLOT(28)                                                              \
	_SLOT(29)                                                              \
	_SLOT(30)                                                              \
	_SLOT(31)                                                              \
	_SLOT(32)                                                              \
	_SLOT(33)                                                              \
	_SLOT(34)                                                              \
	_SLOT(35)                                                              \
	_SLOT(36)                                                              \
	_SLOT(37)                                                              \
	_SLOT(38)                                                              \
	_SLOT(39)                                                              \
	_SLOT(40)                                                              \
	_SLOT(41)                                                              \
	_SLOT(42)                                                              \
	_SLOT(43)                                                              \
	_SLOT(44)                                                              \
	_SLOT(45)                                                              \
	_SLOT(46)                                                              \
	_SLOT(47)                                                              \
	_SLOT(48)                                                              \
	_SLOT(49)                                                              \
	_SLOT(50)                                                              \
	_SLOT(51)                                                              \
	_SLOT(52)                                                              \
	_SLOT(53)                                                              \
	_SLOT(54)                                                              \
	_SLOT(55)                                                              \
	_SLOT(56)                                                              \
	_SLOT(57)                                                              \
	_SLOT(58)                                                              \
	_SLOT(59)                                                              \
	_SLOT(60)                                                              \
	_SLOT(61)                                                              \
	_SLOT(62)                                                              \
	_SLOT(63)                                                              \
	_SLOT(64)                                                              \
	_SLOT(65)                                                              \
	_SLOT(66)                                                              \
	_SLOT(67)                                                              \
	_SLOT(68)                                                              \
	_SLOT(69)                                                              \
	_SLOT(70)                                                              \
	_SLOT(71)                                                              \
	_SLOT(72)                                                              \
	_SLOT(73)                                                              \
	_SLOT(74)                                                              \
	_SLOT(75)                                                              \
	_SLOT(76)                                                              \
	_SLOT(77)                                                              \
	_SLOT(78)                                                              \
	_SLOT(79)                                                              \
	_SLOT(80)                                                              \
	_SLOT(81)                                                              \
	_SLOT(82)                                                              \
	_SLOT(83)                                                              \
	_SLOT(84)                                                              \
	_SLOT(85)                                                              \
	_SLOT(86)                                                              \
	_SLOT(87)                                                              \
	_SLOT(88)                                                              \
	_SLOT(89)                                                              \
	_SLOT(90)                                                              \
	_SLOT(91)                                                              \
	_SLOT(92)                                                              \
	_SLOT(93)                                                              \
	_SLOT(94)                                                              \
	_SLOT(95)                                                              \
	_SLOT(96)                                                              \
	_SLOT(97)                                                              \
	_SLOT(98)                                                              \
	_SLOT(99)                                                              \
	_SLOT(100)                                                             \
	_SLOT(101)                                                             \
	_SLOT(102)                                                             \
	_SLOT(103)                                                             \
	_SLOT(104)                                                             \
	_SLOT(105)                                                             \
	_SLOT(106)                                                             \
	_SLOT(107)                                                             \
	_SLOT(108)                                                             \
	_SLOT(109)                                                             \
	_SLOT(110)                                                             \
	_SLOT(111)                                                             \
	_SLOT(112)                                                             \
	_SLOT(113)                                                             \
	_SLOT(114)                                                             \
	_SLOT(115)                                                             \
	_SLOT(116)                                                             \
	_SLOT(117)                                                             \
	_SLOT(118)                                                             \
	_SLOT(119)                                                             \
	_SLOT(120)                                                             \
	_SLOT(121)                                                             \
	_SLOT(122)                                                             \
	_SLOT(123)                                                             \
	_SLOT(124)                                                             \
	_SLOT(125)                                                             \
	_SLOT(126)                                                             \
	_SLOT(127)

#define _SLOT(N) 1 +
#if (TLKM_SLOTS 0 != PLATFORM_NUM_SLOTS)
#error "PLATFORM_NUM_SLOTS does not match TLKM_SLOTS macro"
#endif
#undef _SLOT

#endif /* TLKM_SLOT_H__ */
