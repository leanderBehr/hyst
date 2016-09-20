'''
Created by Hyst v1.3
Hybrid Automaton in PySim
Converted from file: 
Command Line arguments: -gen nav "-matrix -1.2 0.1 0.2 -1.2 -i_list 3 0 0 6 6 3 0 0 B 4 4 2 1 1 4 4 A 1 1 4 4 6 6 6 4 -width 5 -startx 3.5 -starty 3.5 -noise 0.1" -o nav_fig2b.py -tool pysim "-corners True -legend False -rand 100 -time 20 -title nav_fig2b"
'''

import hybridpy.pysim.simulate as sim
from hybridpy.pysim.simulate import init_list_to_q_list
from hybridpy.pysim.hybrid_automaton import HybridAutomaton
from hybridpy.pysim.hybrid_automaton import HyperRectangle

def define_ha():
    '''make the hybrid automaton and return it'''

    ha = HybridAutomaton()
    ha.variables = ["x", "y", "xvel", "yvel"]


    mode_0_0 = ha.new_mode('mode_0_0')
    mode_0_0.inv = lambda state: state[0] <= 1 and state[1] <= 1
    mode_0_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865476) + 0.1 * (state[3] - -0.7071067811865475), 0.2 * (state[2] - 0.7071067811865476) + -1.2 * (state[3] - -0.7071067811865475)]
    mode_0_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_0 = ha.new_mode('mode_1_0')
    mode_1_0.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] <= 1
    mode_1_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - 1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - 1)]
    mode_1_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_0 = ha.new_mode('mode_2_0')
    mode_2_0.inv = lambda state: state[0] >= 2 and state[0] <= 3 and state[1] <= 1
    mode_2_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - 1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - 1)]
    mode_2_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_3_0 = ha.new_mode('mode_3_0')
    mode_3_0.inv = lambda state: state[0] >= 3 and state[0] <= 4 and state[1] <= 1
    mode_3_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - -1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - -1) + -1.2 * (state[3] - 0)]
    mode_3_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_4_0 = ha.new_mode('mode_4_0')
    mode_4_0.inv = lambda state: state[0] >= 4 and state[1] <= 1
    mode_4_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - -1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - -1) + -1.2 * (state[3] - 0)]
    mode_4_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_1 = ha.new_mode('mode_0_1')
    mode_0_1.inv = lambda state: state[0] <= 1 and state[1] >= 1 and state[1] <= 2
    mode_0_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865476) + 0.1 * (state[3] - -0.7071067811865475), 0.2 * (state[2] - 0.7071067811865476) + -1.2 * (state[3] - -0.7071067811865475)]
    mode_0_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_1 = ha.new_mode('mode_1_1')
    mode_1_1.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 1 and state[1] <= 2
    mode_1_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - 1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - 1)]
    mode_1_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_1 = ha.new_mode('mode_2_1')
    mode_2_1.inv = lambda state: state[0] >= 2 and state[0] <= 3 and state[1] >= 1 and state[1] <= 2
    mode_2_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - 1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - 1)]
    mode_2_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_3_1 = ha.new_mode('mode_3_1')
    mode_3_1.inv = lambda state: state[0] >= 3 and state[0] <= 4 and state[1] >= 1 and state[1] <= 2
    mode_3_1.der = lambda _, state: [0, 0, 0, 0]
    mode_3_1.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_4_1 = ha.new_mode('mode_4_1')
    mode_4_1.inv = lambda state: state[0] >= 4 and state[1] >= 1 and state[1] <= 2
    mode_4_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_4_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_2 = ha.new_mode('mode_0_2')
    mode_0_2.inv = lambda state: state[0] <= 1 and state[1] >= 2 and state[1] <= 3
    mode_0_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_0_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_2 = ha.new_mode('mode_1_2')
    mode_1_2.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 2 and state[1] <= 3
    mode_1_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - 1) + -1.2 * (state[3] - 0)]
    mode_1_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_2 = ha.new_mode('mode_2_2')
    mode_2_2.inv = lambda state: state[0] >= 2 and state[0] <= 3 and state[1] >= 2 and state[1] <= 3
    mode_2_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865475) + 0.1 * (state[3] - 0.7071067811865476), 0.2 * (state[2] - 0.7071067811865475) + -1.2 * (state[3] - 0.7071067811865476)]
    mode_2_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_3_2 = ha.new_mode('mode_3_2')
    mode_3_2.inv = lambda state: state[0] >= 3 and state[0] <= 4 and state[1] >= 2 and state[1] <= 3
    mode_3_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865475) + 0.1 * (state[3] - 0.7071067811865476), 0.2 * (state[2] - 0.7071067811865475) + -1.2 * (state[3] - 0.7071067811865476)]
    mode_3_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_4_2 = ha.new_mode('mode_4_2')
    mode_4_2.inv = lambda state: state[0] >= 4 and state[1] >= 2 and state[1] <= 3
    mode_4_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_4_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_3 = ha.new_mode('mode_0_3')
    mode_0_3.inv = lambda state: state[0] <= 1 and state[1] >= 3 and state[1] <= 4
    mode_0_3.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_0_3.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_3 = ha.new_mode('mode_1_3')
    mode_1_3.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 3 and state[1] <= 4
    mode_1_3.der = lambda _, state: [0, 0, 0, 0]
    mode_1_3.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_2_3 = ha.new_mode('mode_2_3')
    mode_2_3.inv = lambda state: state[0] >= 2 and state[0] <= 3 and state[1] >= 3 and state[1] <= 4
    mode_2_3.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865475) + 0.1 * (state[3] - 0.7071067811865476), 0.2 * (state[2] - 0.7071067811865475) + -1.2 * (state[3] - 0.7071067811865476)]
    mode_2_3.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_3_3 = ha.new_mode('mode_3_3')
    mode_3_3.inv = lambda state: state[0] >= 3 and state[0] <= 4 and state[1] >= 3 and state[1] <= 4
    mode_3_3.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865475) + 0.1 * (state[3] - 0.7071067811865476), 0.2 * (state[2] - 0.7071067811865475) + -1.2 * (state[3] - 0.7071067811865476)]
    mode_3_3.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_4_3 = ha.new_mode('mode_4_3')
    mode_4_3.inv = lambda state: state[0] >= 4 and state[1] >= 3 and state[1] <= 4
    mode_4_3.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_4_3.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_4 = ha.new_mode('mode_0_4')
    mode_0_4.inv = lambda state: state[0] <= 1 and state[1] >= 4
    mode_0_4.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_0_4.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_4 = ha.new_mode('mode_1_4')
    mode_1_4.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 4
    mode_1_4.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - -1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - -1) + -1.2 * (state[3] - 0)]
    mode_1_4.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_4 = ha.new_mode('mode_2_4')
    mode_2_4.inv = lambda state: state[0] >= 2 and state[0] <= 3 and state[1] >= 4
    mode_2_4.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - -1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - -1) + -1.2 * (state[3] - 0)]
    mode_2_4.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_3_4 = ha.new_mode('mode_3_4')
    mode_3_4.inv = lambda state: state[0] >= 3 and state[0] <= 4 and state[1] >= 4
    mode_3_4.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - -1) + 0.1 * (state[3] - 0), 0.2 * (state[2] - -1) + -1.2 * (state[3] - 0)]
    mode_3_4.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_4_4 = ha.new_mode('mode_4_4')
    mode_4_4.inv = lambda state: state[0] >= 4 and state[1] >= 4
    mode_4_4.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0) + 0.1 * (state[3] - -1), 0.2 * (state[2] - 0) + -1.2 * (state[3] - -1)]
    mode_4_4.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    t = ha.new_transition(mode_0_0, mode_1_0)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_0, mode_0_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_0_0)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_2_0)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_0, mode_1_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_0, mode_1_0)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_0, mode_3_0)
    t.guard = lambda state: state[0] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_0, mode_2_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_0, mode_2_0)
    t.guard = lambda state: state[0] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_0, mode_4_0)
    t.guard = lambda state: state[0] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_0, mode_3_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_0, mode_3_0)
    t.guard = lambda state: state[0] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_0, mode_4_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_1_1)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_0_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_1, mode_0_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_0_1)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_2_1)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_1_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_1, mode_1_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_1_1)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_3_1)
    t.guard = lambda state: state[0] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_2_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_1, mode_2_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_1, mode_2_1)
    t.guard = lambda state: state[0] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_1, mode_4_1)
    t.guard = lambda state: state[0] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_1, mode_3_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_1, mode_3_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_1, mode_3_1)
    t.guard = lambda state: state[0] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_1, mode_4_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_1, mode_4_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_2, mode_1_2)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_2, mode_0_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_2, mode_0_3)
    t.guard = lambda state: state[1] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_0_2)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_2_2)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_1_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_2, mode_1_3)
    t.guard = lambda state: state[1] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_1_2)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_3_2)
    t.guard = lambda state: state[0] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_2_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_2, mode_2_3)
    t.guard = lambda state: state[1] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_2, mode_2_2)
    t.guard = lambda state: state[0] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_2, mode_4_2)
    t.guard = lambda state: state[0] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_2, mode_3_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_2, mode_3_3)
    t.guard = lambda state: state[1] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_2, mode_3_2)
    t.guard = lambda state: state[0] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_2, mode_4_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_2, mode_4_3)
    t.guard = lambda state: state[1] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_3, mode_1_3)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_3, mode_0_2)
    t.guard = lambda state: state[1] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_3, mode_0_4)
    t.guard = lambda state: state[1] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_3, mode_0_3)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_3, mode_2_3)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_3, mode_1_2)
    t.guard = lambda state: state[1] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_3, mode_1_4)
    t.guard = lambda state: state[1] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_3, mode_1_3)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_3, mode_3_3)
    t.guard = lambda state: state[0] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_3, mode_2_2)
    t.guard = lambda state: state[1] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_3, mode_2_4)
    t.guard = lambda state: state[1] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_3, mode_2_3)
    t.guard = lambda state: state[0] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_3, mode_4_3)
    t.guard = lambda state: state[0] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_3, mode_3_2)
    t.guard = lambda state: state[1] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_3, mode_3_4)
    t.guard = lambda state: state[1] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_3, mode_3_3)
    t.guard = lambda state: state[0] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_3, mode_4_2)
    t.guard = lambda state: state[1] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_3, mode_4_4)
    t.guard = lambda state: state[1] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_4, mode_1_4)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_0_4, mode_0_3)
    t.guard = lambda state: state[1] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_4, mode_0_4)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_4, mode_2_4)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_1_4, mode_1_3)
    t.guard = lambda state: state[1] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_4, mode_1_4)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_4, mode_3_4)
    t.guard = lambda state: state[0] >= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_2_4, mode_2_3)
    t.guard = lambda state: state[1] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_4, mode_2_4)
    t.guard = lambda state: state[0] <= 3
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_4, mode_4_4)
    t.guard = lambda state: state[0] >= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_3_4, mode_3_3)
    t.guard = lambda state: state[1] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_4, mode_3_4)
    t.guard = lambda state: state[0] <= 4
    t.reset = lambda state: [None, None, None, None]

    t = ha.new_transition(mode_4_4, mode_4_3)
    t.guard = lambda state: state[1] <= 4
    t.reset = lambda state: [None, None, None, None]

    return ha

def define_init_states(ha):
    '''returns a list of (mode, HyperRectangle)'''
    # Variable ordering: [x, y, xvel, yvel]
    rv = []

    r = HyperRectangle([(3.5, 3.5), (3.5, 3.5), (-1, 1), (-1, 1)])
    rv.append((ha.modes['mode_3_3'], r))

    return rv


def simulate(init_states, max_time=20):
    '''simulate the automaton from each initial rect'''

    q_list = init_list_to_q_list(init_states, center=True, star=True, corners=True, rand=100)
    result = sim.simulate_multi(q_list, max_time)

    return result

def plot(result, init_states, filename='plot.png', dim_x=0, dim_y=1):
    '''plot a simulation result to a file'''

    draw_events = len(result) == 1
    shouldShow = False
    sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events, legend=False, title='nav_fig2b', show=shouldShow, init_states=init_states)

if __name__ == '__main__':
    ha = define_ha()
    init_states = define_init_states(ha)
    plot(simulate(init_states), init_states)

